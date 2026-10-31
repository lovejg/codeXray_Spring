package com.codeXray.backend.ai.service;

import com.codeXray.backend.ai.entity.AiJobStatus;
import com.codeXray.backend.ai.repository.AiJobRepository;
import com.codeXray.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/*
 * 오래 멈춘 AI 잡 정리.
 * 컨슈머가 Claude 호출 중에 죽으면(배포, OOM) 잡은 PROCESSING 으로 남는다. PROCESSING 은 다른 컨슈머가 다시 가져가지 않으므로
 * (중복 호출 방지의 대가) 누군가 끝을 내 줘야 한다. 메시지가 사라져 PENDING 으로 남은 잡도 같다.
 * Claude 호출 상한(3분 × 재시도)보다 넉넉한 15분이 지나면 FAILED 로 바꿔 화면의 폴링이 끝나게 한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobReaper {

    static final Duration STALE_AFTER = Duration.ofMinutes(15);

    private final AiJobRepository aiJobRepository;

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void failStaleJobs() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime before = now.minus(STALE_AFTER);
        int processing = aiJobRepository.failStale(AiJobStatus.PROCESSING, before, ErrorCode.AI_TIMEOUT.name(), now);
        int pending = aiJobRepository.failStale(AiJobStatus.PENDING, before, ErrorCode.AI_TIMEOUT.name(), now);
        if (processing + pending > 0) {
            log.warn("멈춘 AI 잡을 FAILED 로 정리. processing={}, pending={}", processing, pending);
        }
    }
}
