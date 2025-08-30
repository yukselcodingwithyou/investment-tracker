package com.yuksel.investmenttracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${spring.mail.username:noreply@investmenttracker.com}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Investment Tracker - Password Reset");
            
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            String emailBody = "Dear User,\n\n" +
                    "You have requested to reset your password for Investment Tracker.\n\n" +
                    "Please click the following link to reset your password:\n" +
                    resetLink + "\n\n" +
                    "This link will expire in 1 hour.\n\n" +
                    "If you did not request this password reset, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "Investment Tracker Team";
            
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to Investment Tracker!");
            
            String emailBody = "Dear " + userName + ",\n\n" +
                    "Welcome to Investment Tracker! Your account has been successfully created.\n\n" +
                    "You can now start tracking your investments by:\n" +
                    "• Adding your acquisitions\n" +
                    "• Monitoring your portfolio performance\n" +
                    "• Analyzing your investment history\n\n" +
                    "Get started here: " + frontendUrl + "\n\n" +
                    "If you have any questions, feel free to contact our support team.\n\n" +
                    "Happy investing!\n" +
                    "Investment Tracker Team";
            
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome emails - they're not critical
            log.warn("Welcome email sending failed, but account creation continues");
        }
    }

    public void sendPortfolioSummaryEmail(String toEmail, String userName, String portfolioSummary) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Investment Tracker - Weekly Portfolio Summary");
            
            String emailBody = "Dear " + userName + ",\n\n" +
                    "Here's your weekly portfolio summary:\n\n" +
                    portfolioSummary + "\n\n" +
                    "For detailed analysis, visit: " + frontendUrl + "\n\n" +
                    "Best regards,\n" +
                    "Investment Tracker Team";
            
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("Portfolio summary email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send portfolio summary email to: {}", toEmail, e);
            // Don't throw exception for summary emails - they're not critical
        }
    }

    public void sendAccountDeletionConfirmationEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Investment Tracker - Account Deletion Confirmation");
            
            String emailBody = "Dear " + userName + ",\n\n" +
                    "Your Investment Tracker account has been successfully deleted.\n\n" +
                    "All your data has been permanently removed from our systems.\n\n" +
                    "If you did not request this deletion or have any concerns, " +
                    "please contact our support team immediately.\n\n" +
                    "Thank you for using Investment Tracker.\n\n" +
                    "Best regards,\n" +
                    "Investment Tracker Team";
            
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("Account deletion confirmation email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send account deletion confirmation email to: {}", toEmail, e);
            // Don't throw exception - account deletion should proceed even if email fails
        }
    }
}