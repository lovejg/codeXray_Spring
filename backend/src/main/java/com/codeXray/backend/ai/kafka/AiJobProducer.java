package com.codeXray.backend.ai.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

// AI 잡 이벤트 발행: 잡 id 를 String 메시지로 토픽에 던진다.
@Component
public class AiJobProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public AiJobProducer(KafkaTemplate<String, String> kafkaTemplate,
                         @Value("${app.kafka.ai-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /*
     * 보내기 결과(future)를 돌려준다. 예전에는 버렸기 때문에 브로커가 죽어 있으면 실패가 future 안에 갇히고
     * 잡은 영원히 PENDING 으로 남았다. 호출한 쪽(AiService)이 실패를 받아 잡을 FAILED 로 끝낸다.
     * 키를 잡 id 로 둬서 파티션을 늘려도 같은 잡의 메시지는 같은 파티션으로 간다
     */
    public CompletableFuture<SendResult<String, String>> publish(Long jobId) {
        String id = String.valueOf(jobId);
        return kafkaTemplate.send(topic, id, id);
    }
}
