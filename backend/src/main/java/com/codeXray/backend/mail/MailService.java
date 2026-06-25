package com.codeXray.backend.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {

    private final String mode;
    private final String from;
    private final String baseUrl;

    public MailService(
            @Value("${app.mail.mode}") String mode,
            @Value("${app.mail.from}") String from,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.mode = mode;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    public void sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        String subject = "[codeXray] 이메일 인증을 완료해 주세요";
        String body = """
                안녕하세요, codeXray입니다.
                아래 링크를 눌러 이메일 인증을 완료해 주세요. (24시간 이내 유효)

                %s
                """.formatted(link);

        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        if ("smtp".equalsIgnoreCase(mode)) {
            // TODO(추후): JavaMailSender 주입해서 실제 SMTP 발송 구현
            log.warn("SMTP 모드이지만 아직 미구현입니다. 메일을 보내지 못했습니다. to={}", to);
            return;
        }

        // console 모드(개발): 실제 발송 대신 콘솔에 출력
        log.info("""

                ====================== [MAIL:console] ======================
                from   : {}
                to     : {}
                subject: {}
                ------------------------------------------------------------
                {}
                ============================================================
                """, from, to, subject, body);
    }
}
