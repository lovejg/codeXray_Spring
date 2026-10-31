package com.codeXray.backend.ai.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AnthropicConfig {

    // 키가 없어도 앱이 뜨도록 placeholder로 빌드(실제 호출 시 AiService가 키 유무를 검사).
    // timeout: 요청 한 번의 상한. maxRetries: 429·5xx·연결 오류에 SDK 가 알아서 다시 시도하는 횟수.
    // 컨슈머 스레드에서 돌므로 사용자 요청을 붙잡지는 않지만, 상한이 없으면 잡이 PROCESSING 에 오래 머문다
    @Bean
    public AnthropicClient anthropicClient(@Value("${app.anthropic.api-key:}") String apiKey,
                                           @Value("${app.anthropic.timeout-seconds:180}") long timeoutSeconds,
                                           @Value("${app.anthropic.max-retries:2}") int maxRetries) {
        String key = (apiKey == null || apiKey.isBlank()) ? "sk-ant-placeholder" : apiKey;
        return AnthropicOkHttpClient.builder()
                .apiKey(key)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                .build();
    }
}
