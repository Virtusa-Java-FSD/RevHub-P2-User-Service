package com.revhub.service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username:noreply@revhub.com}")
    private String fromEmail;
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("RevHub - Password Reset OTP");
            String htmlContent = buildOtpEmailTemplate(otp);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email. Please try again later.");
        }
    }
    private String buildOtpEmailTemplate(String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f4f4f4;
                            margin: 0;
                            padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 40px auto;
                            background-color: #ffffff;
                            border-radius: 12px;
                            overflow: hidden;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            padding: 30px;
                            text-align: center;
                            color: white;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 600;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .content h2 {
                            color: #333;
                            font-size: 22px;
                            margin-bottom: 20px;
                        }
                        .content p {
                            color: #666;
                            line-height: 1.6;
                            margin-bottom: 20px;
                        }
                        .otp-box {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            font-size: 36px;
                            font-weight: bold;
                            letter-spacing: 8px;
                            text-align: center;
                            padding: 20px;
                            border-radius: 8px;
                            margin: 30px 0;
                        }
                        .warning {
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .warning p {
                            margin: 0;
                            color: #856404;
                            font-size: 14px;
                        }
                        .footer {
                            background-color: #f8f9fa;
                            padding: 20px 30px;
                            text-align: center;
                            color: #6c757d;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔐 RevHub</h1>
                        </div>
                        <div class="content">
                            <h2>Password Reset Request</h2>
                            <p>Hello,</p>
                            <p>We received a request to reset your password. Use the OTP below to verify your identity and reset your password:</p>
                            <div class="otp-box">
                                """
                + otp
                + """
                                    </div>
                                    <div class="warning">
                                        <p><strong>⚠️ Important:</strong> This OTP will expire in 10 minutes. If you didn't request this password reset, please ignore this email.</p>
                                    </div>
                                    <p>For your security, never share this OTP with anyone.</p>
                                    <p>Best regards,<br>The RevHub Team</p>
                                </div>
                                <div class="footer">
                                    <p>This is an automated message, please do not reply to this email.</p>
                                    <p>&copy; 2024 RevHub. All rights reserved.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """;
    }
}
