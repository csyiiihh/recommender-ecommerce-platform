package org.example.springboot.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    @Value("${user.fromEmail}")
    private String fromEmail;

    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * 发送简单文本邮件
     */
    public void sendSimpleEmail(String toEmail, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);

            javaMailSender.send(message);
            LOGGER.info("简单邮件发送成功：{} -> {}", fromEmail, toEmail);
        } catch (Exception e) {
            LOGGER.error("简单邮件发送失败：{}", e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 发送HTML格式邮件
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);  // true表示发送HTML

            javaMailSender.send(message);
            LOGGER.info("HTML邮件发送成功：{} -> {}", fromEmail, toEmail);
        } catch (Exception e) {
            LOGGER.error("HTML邮件发送失败：{}", e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 发送注册验证码邮件
     */
    public void sendRegisterCode(String toEmail, String code) {
        String subject = "在线销售平台 - 注册验证码";
        String content = String.format(
                "您的注册验证码为：%s，有效期10分钟，请勿泄露给他人。\n\n" +
                        "如果不是您本人操作，请忽略此邮件。",
                code
        );
        sendSimpleEmail(toEmail, subject, content);
    }

    /**
     * 发送找回密码验证码邮件
     */
    public void sendResetPasswordCode(String toEmail, String code) {
        String subject = "在线销售平台 - 密码重置验证码";
        String content = String.format(
                "您的密码重置验证码为：%s，有效期10分钟，请勿泄露给他人。\n\n" +
                        "如果不是您本人操作，请立即联系客服。",
                code
        );
        sendSimpleEmail(toEmail, subject, content);
    }

    /**
     * 发送HTML格式的验证码邮件（更美观）
     */
    public void sendHtmlVerificationCode(String toEmail, String code, String type) {
        String subject = "在线销售平台 - " + ("register".equals(type) ? "注册" : "密码重置") + "验证码";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    .container { max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .code { font-size: 24px; font-weight: bold; color: #4CAF50; text-align: center; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>在线销售平台</h2>
                    </div>
                    <div class="content">
                        <p>尊敬的用户，您好！</p>
                        <p>您正在进行%s操作，验证码为：</p>
                        <div class="code">%s</div>
                        <p>请在10分钟内完成验证。</p>
                        <p>如果这不是您的操作，请忽略此邮件。</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复</p>
                        <p>© 2025 在线销售平台</p>
                    </div>
                </div>
            </body>
            </html>
            """, "register".equals(type) ? "注册" : "密码重置", code);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }
}