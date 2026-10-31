package com.codeXray.backend.ai.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    // DeadLetterPublishingRecoverer 의 기본 규칙: 실패한 메시지를 "<원래 토픽>.DLT" 의 같은 파티션 번호로 보낸다
    public static final String DLT_SUFFIX = ".DLT";

    // 브로커가 켜져 있으면 시작 시 KafkaAdmin 이 이 토픽을 자동 생성.
    @Bean
    public NewTopic aiJobsTopic(@Value("${app.kafka.ai-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    // 끝내 처리하지 못한 메시지가 쌓이는 곳. 원래 토픽과 파티션 수를 맞춘다(같은 파티션 번호로 보내므로)
    @Bean
    public NewTopic aiJobsDeadLetterTopic(@Value("${app.kafka.ai-topic}") String topic) {
        return TopicBuilder.name(topic + DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    // String 키/값 전용 KafkaTemplate. Boot 자동 빈은 KafkaTemplate<?,?> 라
    // <String,String> 주입이 안 되므로 명시적으로 정의.
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 브로커가 죽어 있을 때 send() 가 요청 스레드를 붙잡는 시간. 기본 60초면 사용자 요청이 1분 멈춘다
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5_000);
        // 보내기를 포기하고 실패를 알리는 시간(재시도 포함). request.timeout + linger 보다 커야 한다
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5_000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15_000);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(pf);
    }

    /*
     * @KafkaListener 의 오류 처리기. Spring Boot 가 이 빈을 리스너 컨테이너에 자동으로 붙인다.
     *  - 1초 간격으로 2번 더 시도(DB 순간 장애 같은 일시적 실패)
     *  - 그래도 실패하면 DLT 토픽으로 옮기고 오프셋을 넘긴다(한 메시지가 뒤 메시지를 영원히 막지 않게)
     *  - 숫자가 아닌 jobId 는 다시 해도 같으니 바로 DLT
     * 예전에는 기본 처리기가 로그 한 줄 없이 건너뛰었다
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));
        handler.addNotRetryableExceptions(NumberFormatException.class);
        return handler;
    }
}
