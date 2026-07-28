package com.codeXray.backend.ai.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    public void publish(Long jobId) {
        kafkaTemplate.send(topic, String.valueOf(jobId));
    }
}
