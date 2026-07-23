package com.codeXray.backend.ai.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    // 키가 없어도 앱이 뜨도록 placeholder로 빌드(실제 호출 시 AiService가 키 유무를 검사).
    @Bean
    public AnthropicClient anthropicClient(@Value("${app.anthropic.api-key:}") String apiKey) {
        String key = (apiKey == null || apiKey.isBlank()) ? "sk-ant-placeholder" : apiKey;
        return AnthropicOkHttpClient.builder()
                .apiKey(key)
                .build();
    }
}
