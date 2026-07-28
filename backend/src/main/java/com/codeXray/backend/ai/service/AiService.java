package com.codeXray.backend.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.codeXray.backend.ai.dto.AiJobResponse;
import com.codeXray.backend.ai.dto.AiTaskType;
import com.codeXray.backend.ai.entity.AiJob;
import com.codeXray.backend.ai.entity.AiJobKind;
import com.codeXray.backend.ai.entity.AiJobStatus;
import com.codeXray.backend.ai.kafka.AiJobProducer;
import com.codeXray.backend.ai.repository.AiJobRepository;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.repository.ProblemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final String MODEL = "claude-opus-4-8";

    private static final String SYSTEM_PROMPT =
            "당신은 알고리즘 코딩테스트 전문 AI 어시스턴트입니다. " +
            "코드 분석, 최적화, 알고리즘 태그 추천, 난이도 평가를 전문으로 합니다. " +
            "답변은 한국어로 작성하고, 명확하고 구체적으로 설명하세요.";

    private static final String HINT_SYSTEM_PROMPT =
            "당신은 알고리즘 학습을 돕는 튜터입니다. 학습자가 스스로 답을 찾도록 '힌트'만 제공합니다. " +
            "완성된 정답 코드나 핵심 로직의 코드 스니펫은 절대 제공하지 마세요. " +
            "아이디어와 접근 방향을 말로 설명하고, 한국어로 작성하세요.";

    private final AnthropicClient anthropicClient;
    private final AiRateLimiter rateLimiter;
    private final ProblemRepository problemRepository;
    private final AiJobRepository aiJobRepository;
    private final AiJobProducer aiJobProducer;
    private final String apiKey;

    public AiService(AnthropicClient anthropicClient,
                     AiRateLimiter rateLimiter,
                     ProblemRepository problemRepository,
                     AiJobRepository aiJobRepository,
                     AiJobProducer aiJobProducer,
                     @Value("${app.anthropic.api-key:}") String apiKey) {
        this.anthropicClient = anthropicClient;
        this.rateLimiter = rateLimiter;
        this.problemRepository = problemRepository;
        this.aiJobRepository = aiJobRepository;
        this.aiJobProducer = aiJobProducer;
        this.apiKey = apiKey;
    }

    // ── 요청(프로듀서): 잡을 PENDING 으로 저장하고 Kafka 에 발행 → jobId 반환 ──
    public Long requestAnalyze(Long userId, AiTaskType task, String code, String language, String problemTitle) {
        rateLimiter.checkAndIncrement(userId); // 한도 초과면 여기서 429
        String prompt = buildPrompt(task, code, language, problemTitle);
        return enqueue(userId, AiJobKind.ANALYZE, SYSTEM_PROMPT, prompt);
    }

    public Long requestHint(Long userId, Long problemId, String partialCode) {
        rateLimiter.checkAndIncrement(userId);
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        List<String> tags = problemRepository.findTagNamesByProblemId(problemId);
        String prompt = buildHintPrompt(problem.getTitle(), tags, partialCode);
        return enqueue(userId, AiJobKind.HINT, HINT_SYSTEM_PROMPT, prompt);
    }

    private Long enqueue(Long userId, AiJobKind kind, String system, String prompt) {
        AiJob job = aiJobRepository.save(AiJob.builder()
                .userId(userId).kind(kind).systemPrompt(system).userPrompt(prompt).build());
        aiJobProducer.publish(job.getId());
        return job.getId();
    }

    // ── 처리(컨슈머): Claude 를 호출해 결과를 저장. 트랜잭션 안에서 외부호출 하지 않도록
    //    엔티티를 로드→수정→save 로 갱신(긴 트랜잭션 회피). 재전달 대비 PENDING 만 처리. ──
    public void process(Long jobId) {
        AiJob job = aiJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != AiJobStatus.PENDING) return;
        try {
            String result = callClaude(job.getSystemPrompt(), job.getUserPrompt());
            job.markDone(result);
        } catch (BusinessException e) {
            job.markFailed(e.getErrorCode().name());
        } catch (RuntimeException e) {
            job.markFailed(ErrorCode.AI_UNAVAILABLE.name());
        }
        aiJobRepository.save(job);
    }

    // ── 조회(폴링): 내 잡 상태/결과 ──
    public AiJobResponse getJob(Long userId, Long jobId) {
        AiJob job = aiJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return AiJobResponse.from(job);
    }

    // ── Claude 호출 공통부 ──
    private String callClaude(String system, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
        }
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(2048L)
                    .system(system)
                    .addUserMessage(userPrompt)
                    .build();

            Message message = anthropicClient.messages().create(params);
            return message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .collect(Collectors.joining());
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
        }
    }

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

    private String buildHintPrompt(String title, List<String> tags, String partialCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("문제명: ").append(title).append('\n');
        if (tags != null && !tags.isEmpty()) {
            sb.append("알고리즘 태그: ").append(String.join(", ", tags)).append('\n');
        }
        if (partialCode != null && !partialCode.isBlank()) {
            sb.append("\n학습자가 지금까지 작성한(막힌) 코드:\n```\n").append(partialCode).append("\n```\n");
        }
        sb.append("\n이 문제가 잘 안 풀립니다. 정답 코드는 절대 주지 말고, 스스로 풀 수 있도록 아래 3단계로 나눠 힌트만 주세요.\n");
        sb.append("각 단계는 마크다운 소제목(##)으로 구분하고, 뒤로 갈수록 조금씩 더 구체적으로:\n");
        sb.append("## 1단계 · 접근 방향  — 어떤 유형/알고리즘으로 접근할지 큰 힌트\n");
        sb.append("## 2단계 · 핵심 아이디어 — 상태 정의·자료구조 등 결정적 아이디어(코드는 금지)\n");
        sb.append("## 3단계 · 구현 힌트 — 구현 시 주의점을 말로 설명(정답 코드·핵심 코드 스니펫 금지)\n");
        return sb.toString();
    }
}
