package com.codeXray.backend.ai.kafka;

import com.codeXray.backend.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// AI 잡 이벤트 구독: jobId 를 받아 실제 Claude 처리를 수행(느린 작업을 요청 경로 밖으로 분리).
@Component
@RequiredArgsConstructor
public class AiJobConsumer {

    private final AiService aiService;

    @KafkaListener(topics = "${app.kafka.ai-topic}")
    public void onMessage(String jobId) {
        aiService.process(Long.valueOf(jobId));
    }
}
