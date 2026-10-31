package com.codeXray.backend.ai.repository;

import com.codeXray.backend.ai.entity.AiJob;
import com.codeXray.backend.ai.entity.AiJobKind;
import com.codeXray.backend.ai.entity.AiJobStatus;
import com.codeXray.backend.config.QueryDslConfig;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// AI 잡을 컨슈머가 "한 번만" 가져가는지. Kafka 가 같은 메시지를 두 번 줘도 유료 Claude 호출이 한 번이어야 한다.
// 조건부 UPDATE 의 행 수로 판정하므로 실제 Postgres 에 대고 돌린다
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
class AiJobTransitionTest {

    @Autowired UserRepository userRepository;
    @Autowired AiJobRepository aiJobRepository;

    private Long jobId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .email("transition@test.example").password("x").nickname("transition-test")
                .role(UserRole.USER).provider(AuthProvider.LOCAL).build());
        jobId = aiJobRepository.saveAndFlush(AiJob.builder()
                .userId(user.getId()).kind(AiJobKind.ANALYZE)
                .systemPrompt("system").userPrompt("print(1)").build()).getId();
    }

    @Test
    @DisplayName("PENDING 잡은 한 번만 PROCESSING 으로 가져갈 수 있다(두 번째 시도는 0행)")
    void claimOnlyOnce() {
        LocalDateTime now = LocalDateTime.now();

        int first = aiJobRepository.transition(jobId, AiJobStatus.PENDING, AiJobStatus.PROCESSING, now);
        int second = aiJobRepository.transition(jobId, AiJobStatus.PENDING, AiJobStatus.PROCESSING, now);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(aiJobRepository.findById(jobId).orElseThrow().getStatus()).isEqualTo(AiJobStatus.PROCESSING);
    }

    @Test
    @DisplayName("발행 실패 처리는 아직 PENDING 일 때만 FAILED 로 바꾼다(이미 처리 중인 잡은 건드리지 않는다)")
    void failIfOnlyFromExpectedStatus() {
        LocalDateTime now = LocalDateTime.now();
        aiJobRepository.transition(jobId, AiJobStatus.PENDING, AiJobStatus.PROCESSING, now);

        int changed = aiJobRepository.failIf(jobId, AiJobStatus.PENDING, "AI_UNAVAILABLE", now);

        assertThat(changed).isZero();
        assertThat(aiJobRepository.findById(jobId).orElseThrow().getStatus()).isEqualTo(AiJobStatus.PROCESSING);
    }

    @Test
    @DisplayName("오래 멈춘 PROCESSING 잡은 FAILED(AI_TIMEOUT)로 정리되고, 최근 잡은 그대로 둔다")
    void failStaleProcessing() {
        LocalDateTime longAgo = LocalDateTime.now().minusHours(1);
        aiJobRepository.transition(jobId, AiJobStatus.PENDING, AiJobStatus.PROCESSING, longAgo);

        int recentCutoff = aiJobRepository.failStale(AiJobStatus.PROCESSING, longAgo.minusMinutes(1), "AI_TIMEOUT", LocalDateTime.now());
        int staleCutoff = aiJobRepository.failStale(AiJobStatus.PROCESSING, longAgo.plusMinutes(1), "AI_TIMEOUT", LocalDateTime.now());

        assertThat(recentCutoff).isZero();
        assertThat(staleCutoff).isEqualTo(1);
        AiJob job = aiJobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(job.getErrorCode()).isEqualTo("AI_TIMEOUT");
    }
}
