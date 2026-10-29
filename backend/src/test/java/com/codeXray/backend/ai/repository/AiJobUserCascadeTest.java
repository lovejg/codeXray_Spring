package com.codeXray.backend.ai.repository;

import com.codeXray.backend.ai.entity.AiJob;
import com.codeXray.backend.ai.entity.AiJobKind;
import com.codeXray.backend.config.QueryDslConfig;
import com.codeXray.backend.user.entity.AuthProvider;
import com.codeXray.backend.user.entity.User;
import com.codeXray.backend.user.entity.UserRole;
import com.codeXray.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

// 탈퇴하면 AI 요청 기록(붙여넣은 코드가 들어 있다)도 지워지는지. 개인정보처리방침에 그렇게 적었다.
// 지우는 건 DB 의 ON DELETE CASCADE(V11)라서 실제 Postgres 에 대고 돌린다
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
class AiJobUserCascadeTest {

    @Autowired UserRepository userRepository;
    @Autowired AiJobRepository aiJobRepository;
    @Autowired EntityManager em;

    @Test
    @DisplayName("유저를 지우면 그 유저의 AI 요청 기록도 지워진다")
    void deletingUserDeletesAiJobs() {
        User user = userRepository.save(User.builder()
                .email("cascade@test.example").password("x").nickname("cascade-test")
                .role(UserRole.USER).provider(AuthProvider.LOCAL).build());
        Long jobId = aiJobRepository.save(AiJob.builder()
                .userId(user.getId()).kind(AiJobKind.ANALYZE)
                .systemPrompt("system").userPrompt("print(1)").build()).getId();
        em.flush();
        em.clear();

        userRepository.deleteById(user.getId());
        em.flush();
        // 영속성 컨텍스트를 비워야 DB 를 다시 읽는다(DB 가 지운 행은 JPA 가 모른다)
        em.clear();

        assertThat(aiJobRepository.findById(jobId)).isEmpty();
    }
}
