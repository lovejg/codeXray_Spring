package com.codeXray.backend.ai.kafka;

import com.codeXray.backend.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// AI 잡 이벤트 구독: jobId 를 받아 실제 Claude 처리를 수행(느린 작업을 요청 경로 밖으로 분리).
// 여기서 던진 예외는 KafkaTopicConfig 의 오류 처리기가 받는다: 2번 더 시도하고, 그래도 안 되면 DLT 토픽으로 옮긴다.
// 숫자가 아닌 메시지는 다시 해도 같으므로 재시도 없이 바로 DLT 로 간다(로그 없이 사라지던 문제)
@Component
@RequiredArgsConstructor
public class AiJobConsumer {

    private final AiService aiService;

    @KafkaListener(topics = "${app.kafka.ai-topic}")
    public void onMessage(String jobId) {
        aiService.process(Long.valueOf(jobId));
    }
}
