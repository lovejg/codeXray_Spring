package com.codeXray.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @Async 활성화. 메일 발송처럼 오래 걸리는 작업을 요청 스레드에서 분리해
// HTTP 응답이 SMTP 전송을 기다리지 않도록 한다.
@Configuration
@EnableAsync
public class AsyncConfig {
}
