package com.codeXray.backend.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.codeXray.backend.ai.dto.AiTaskType;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AiService {

    // SDK 상수(Model.CLAUDE_*)는 릴리스마다 최신 모델이 빠질 수 있어 문자열 ID를 직접 사용
    private static final String MODEL = "claude-opus-4-8";

    private static final String SYSTEM_PROMPT =
            "당신은 알고리즘 코딩테스트 전문 AI 어시스턴트입니다. " +
            "코드 분석, 최적화, 알고리즘 태그 추천, 난이도 평가를 전문으로 합니다. " +
            "답변은 한국어로 작성하고, 명확하고 구체적으로 설명하세요.";

    private final AnthropicClient anthropicClient;
    private final AiRateLimiter rateLimiter;
    private final String apiKey;

    public AiService(AnthropicClient anthropicClient,
                     AiRateLimiter rateLimiter,
                     @Value("${app.anthropic.api-key:}") String apiKey) {
        this.anthropicClient = anthropicClient;
        this.rateLimiter = rateLimiter;
        this.apiKey = apiKey;
    }

    public String analyze(Long userId, AiTaskType task, String code, String language, String problemTitle) {
        // 1) 먼저 일일 한도 체크(초과 시 429) — 요청 자체를 카운트
        rateLimiter.checkAndIncrement(userId);

        // 2) 키가 없으면 호출 불가(503)
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
        }

        // 3) 프롬프트 구성 후 Claude 호출
        String prompt = buildPrompt(task, code, language, problemTitle);
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(2048L)
                    .system(SYSTEM_PROMPT)
                    .addUserMessage(prompt)
                    .build();

            Message message = anthropicClient.messages().create(params);

            // content는 블록 배열 → text 블록만 골라 이어붙임
            return message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .collect(Collectors.joining());
        } catch (RuntimeException e) {
            // API 오류(인증/과금/네트워크 등) → 503으로 통일
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
        }
    }

    // task별 프롬프트 템플릿 (Node ai.service의 buildPrompt 대응)
    private String buildPrompt(AiTaskType task, String code, String language, String problemTitle) {
        String lang = (language == null || language.isBlank()) ? "python" : language;
        String title = (problemTitle == null || problemTitle.isBlank()) ? "" : "문제명: " + problemTitle + "\n";
        String fenced = "\n\n```" + lang + "\n" + code + "\n```";

        return switch (task) {
            case OPTIMIZE -> title
                    + "다음 " + lang + " 코드를 시간복잡도와 공간복잡도 측면에서 최적화해주세요.\n"
                    + "개선 포인트와 최적화된 코드를 함께 제시해주세요." + fenced;
            case EXPLAIN -> title
                    + "다음 " + lang + " 코드의 풀이 논리를 단계별로 설명해주세요.\n"
                    + "어떤 알고리즘을 사용했는지, 핵심 자료구조가 무엇인지 포함해서 설명해주세요." + fenced;
        };
    }
}
