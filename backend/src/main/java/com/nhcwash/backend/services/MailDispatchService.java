package com.nhcwash.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Async SMTP delivery so HTTP threads (e.g. staff status updates) are not blocked on the mail server.
 */
@Service
public class MailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(MailDispatchService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${nhcwash.mail.from:noreply@nhcwash.local}")
    private String fromAddress;

    public MailDispatchService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Async
    public void sendHtml(String to, String subject, String htmlBody) {
        sendHtmlSync(to, subject, htmlBody, null, null, null);
    }

    /**
     * Sends on the caller thread so template/SMTP failures surface to the caller (account deletion must not be fire-and-forget).
     */
    public void sendHtmlBlocking(String to, String subject, String htmlBody) {
        sendHtmlSync(to, subject, htmlBody, null, null, null);
    }

    /** @param replyTo optional visitor address for {@code Reply-To} header */
    @Async
    public void sendHtml(String to, String subject, String htmlBody, String replyTo) {
        sendHtmlSync(to, subject, htmlBody, null, null, replyTo);
    }

    @Async
    public void sendHtmlWithAttachment(String to, String subject, String htmlBody, String attachmentFilename,
            byte[] attachmentBytes) {
        sendHtmlSync(to, subject, htmlBody, attachmentFilename, attachmentBytes, null);
    }

    /** Plain-text body ({@code isHtml} = {@code false}). */
    @Async
    public void sendPlainText(String to, String subject, String textBody, String replyTo) {
        sendPlainTextSync(to, subject, textBody, replyTo);
    }

    private void sendPlainTextSync(String to, String subject, String textBody, String replyTo) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.debug("JavaMailSender not configured — skip plain-text email to {}", to);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo.trim());
            }
            helper.setSubject(subject);
            helper.setText(textBody, false);
            log.info("Sending plain-text email to: {}", to);
            sender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send plain-text email to {}: {}", to, e.getMessage());
        }
    }

    private void sendHtmlSync(String to, String subject, String htmlBody, String attachmentFilename,
            byte[] attachmentBytes, String replyTo) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            System.err.println("====== EMAIL FAILED TO SEND: JavaMailSender bean is not configured (SMTP missing?) to=" + to
                    + " subject=" + subject + " ======");
            log.warn("JavaMailSender not configured — skip HTML email to {}", to);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo.trim());
            }
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (attachmentBytes != null && attachmentBytes.length > 0) {
                String name = attachmentFilename != null && !attachmentFilename.isBlank() ? attachmentFilename
                        : "facture.pdf";
                helper.addAttachment(name, new ByteArrayResource(attachmentBytes));
            }
            log.info("Sending email to: {}", to);
            sender.send(message);
        } catch (Exception e) {
            System.err.println("====== EMAIL FAILED TO SEND ======");
            e.printStackTrace();
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
