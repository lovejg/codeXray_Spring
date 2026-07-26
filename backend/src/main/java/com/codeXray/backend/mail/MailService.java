package com.codeXray.backend.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {

    private final String mode;
    private final String from;
    private final String baseUrl;
    // JavaMailSender는 spring.mail.* 가 있어야 자동설정됨. console 모드/미설정에서도
    // 앱이 뜨도록 ObjectProvider로 지연 주입(없으면 null).
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public MailService(
            @Value("${app.mail.mode}") String mode,
            @Value("${app.mail.from}") String from,
            @Value("${app.base-url}") String baseUrl,
            ObjectProvider<JavaMailSender> mailSenderProvider
    ) {
        this.mode = mode;
        this.from = from;
        this.baseUrl = baseUrl;
        this.mailSenderProvider = mailSenderProvider;
    }

    // @Async: 별도 스레드에서 발송 → 회원가입/재발송 응답이 SMTP 전송을 기다리지 않음.
    // (토큰 문자열만 넘겨받아 DB를 건드리지 않으므로 트랜잭션 커밋 여부와 무관하게 안전)
    @Async
    public void sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        String subject = "[codeXray] 이메일 인증을 완료해 주세요";

        // 메일 클라이언트가 HTML을 못 볼 때 대비한 순수 텍스트 대체본
        String text = """
                안녕하세요, codeXray입니다.
                아래 링크를 눌러 이메일 인증을 완료해 주세요. (24시간 이내 유효)

                %s
                """.formatted(link);

        String html = buildVerificationHtml(link);

        send(to, subject, text, html);
    }

    /** 이메일 인증 메일의 HTML 본문. 이메일 클라이언트 호환을 위해 스타일은 전부 inline. */
    private String buildVerificationHtml(String link) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f3f4f6;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:480px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#4f46e5,#6366f1);padding:32px 40px;">
                              <span style="color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.5px;">code<span style="color:#c7d2fe;">Xray</span></span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:40px;">
                              <h1 style="margin:0 0 12px;font-size:20px;color:#111827;font-weight:700;">이메일 인증을 완료해 주세요</h1>
                              <p style="margin:0 0 28px;font-size:15px;line-height:1.7;color:#4b5563;">
                                codeXray 가입을 환영합니다! 아래 버튼을 눌러 이메일 인증을 완료하면 모든 기능을 사용할 수 있어요.
                              </p>
                              <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto 28px;">
                                <tr>
                                  <td align="center" style="border-radius:10px;background:linear-gradient(135deg,#4f46e5,#6366f1);">
                                    <a href="%s" target="_blank"
                                       style="display:inline-block;padding:14px 36px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:10px;">
                                      이메일 인증하기
                                    </a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0 0 8px;font-size:13px;color:#9ca3af;">
                                버튼이 눌리지 않으면 아래 주소를 브라우저에 붙여넣어 주세요.
                              </p>
                              <p style="margin:0 0 24px;font-size:13px;word-break:break-all;">
                                <a href="%s" target="_blank" style="color:#4f46e5;text-decoration:underline;">%s</a>
                              </p>
                              <p style="margin:0;padding-top:20px;border-top:1px solid #f0f0f0;font-size:12px;color:#9ca3af;">
                                이 링크는 24시간 동안만 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 40px;background-color:#fafafa;">
                              <p style="margin:0;font-size:12px;color:#b0b0b0;">© codeXray · 알고리즘 문제 풀이 학습 플랫폼</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(link, link, link);
    }

    private void send(String to, String subject, String text, String html) {
        if ("smtp".equalsIgnoreCase(mode)) {
            sendSmtp(to, subject, text, html);
            return;
        }

        // console 모드(개발): 실제 발송 대신 콘솔에 링크 출력
        log.info("""

                ====================== [MAIL:console] ======================
                from   : {}
                to     : {}
                subject: {}
                ------------------------------------------------------------
                {}
                ============================================================
                """, from, to, subject, text);
    }

    private void sendSmtp(String to, String subject, String text, String html) {
        // 자동설정된 JavaMailSender를 꺼냄. spring.mail.host 등이 비어있으면 null일 수 있음.
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.error("MAIL_MODE=smtp 이지만 JavaMailSender가 없습니다. spring.mail.* (SMTP_HOST/USER/PASS) 설정을 확인하세요. to={}", to);
            return;
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            // multipart=true → 텍스트/HTML 대체본을 함께 담을 수 있음
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            // (plainText, htmlText) 순서. HTML 지원 클라이언트는 html을, 아니면 text를 표시.
            helper.setText(text, html);

            sender.send(message);
            log.info("인증 메일 발송 완료. to={}", to);
        } catch (Exception e) {
            // 발송 실패해도 가입 트랜잭션은 이미 커밋됨 → 예외를 던지지 않고 로그만.
            // (사용자는 로그인 시 재발송 요청 가능)
            log.error("인증 메일 발송 실패. to={}, err={}", to, e.getMessage(), e);
        }
    }
}
