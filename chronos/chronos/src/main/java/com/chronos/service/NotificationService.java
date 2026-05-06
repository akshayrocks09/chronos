package com.chronos.service;

import com.chronos.entity.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NotificationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM_EMAIL:}")
    private String resendFromEmail;

    /**
     * Sends an email notification to the job owner when a job
     * permanently fails (exhausted all retries).
     */
    @Async
    public void notifyJobPermanentlyFailed(Job job) {
        String ownerEmail = job.getUser().getEmail();
        log.info("Sending failure notification for job '{}' to {}", job.getName(), ownerEmail);
        String subject = "[Chronos] Job '" + job.getName() + "' permanently failed";
        String body = buildFailureEmailBody(job);

        try {
            sendEmailDynamically(ownerEmail, subject, body);
            log.info("Failure notification sent to {}", ownerEmail);
        } catch (Exception e) {
            log.warn("Could not send failure notification email: {}", e.getMessage());
        }
    }

    /**
     * Generic email sender used by the SEND_EMAIL job type.
     * Re-throws on failure so the job can fail and retry.
     */
    public void sendCustomEmail(String to, String subject, String body) {
        log.info("Sending custom email to {} | subject: {}", to, subject);
        try {
            sendEmailDynamically(to, subject, body);
            log.info("Custom email sent to {}", to);
        } catch (Exception e) {
            log.warn("Could not send custom email: {}", e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    private void sendEmailDynamically(String to, String subject, String body) {
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            sendViaResendApi(to, subject, body);
        } else {
            sendViaSmtp(to, subject, body);
        }
    }

    private void sendViaResendApi(String to, String subject, String body) {
        log.info("Dispatching email via Resend API to {}", to);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resendApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String sender = (resendFromEmail != null && !resendFromEmail.trim().isEmpty()) 
                    ? resendFromEmail 
                    : "noreply@chronos.local";

            Map<String, Object> payload = Map.of(
                "from", sender,
                "to", to,
                "subject", subject,
                "html", body
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.resend.com/emails", 
                entity, 
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Resend API returned non-success status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email via Resend API: {}", e.getMessage());
            throw e;
        }
    }

    private void sendViaSmtp(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String buildFailureEmailBody(Job job) {
        return """
                Hello %s,

                Your job has permanently failed after exhausting all retry attempts.

                Job Details:
                  Name        : %s
                  ID          : %d
                  Type        : %s
                  Attempts    : %d / %d
                  Last Error  : %s
                  Last Run    : %s

                Please log in to the Chronos dashboard to review and reschedule the job.

                — Chronos Job Scheduler
                """.formatted(
                job.getUser().getUsername(),
                job.getName(),
                job.getId(),
                job.getJobType(),
                job.getRetryCount(),
                job.getMaxRetries(),
                job.getLastErrorMessage(),
                job.getLastExecutedAt()
        );
    }
}
