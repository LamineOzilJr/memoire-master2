package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    public void sendNewDemandeToManagerEmail(String to, String managerName, String employeeName, String type, String start, String end) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Nouvelle demande de congé");
        String content = String.format("<p>Bonjour %s,</p><p>%s a soumis une demande de %s du %s au %s.</p><p>Merci de traiter.</p>", managerName, employeeName, type, start, end);
        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendManagerDecisionEmail(String to, String employeeName, String type, String start, String end, String decision, String commentaire) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Décision du manager concernant votre demande");
        String content = String.format("<p>Bonjour %s,</p><p>La demande de %s du %s au %s a été %s par votre manager.</p><p>Commentaire: %s</p>", employeeName, type, start, end, decision, commentaire == null ? "" : commentaire);
        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendNewDemandeToRhEmail(String to, String rhName, String employeeName, String type, String start, String end) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Nouvelle demande de congé à traiter (RH)");
        String content = String.format("<p>Bonjour %s,</p><p>Une demande de %s du %s au %s de %s a été approuvée par le manager et attend votre traitement.</p>", rhName, type, start, end, employeeName);
        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendRhDecisionEmail(String to, String employeeName, String type, String start, String end, String decision, String commentaire) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Décision RH concernant votre demande");
        String content = String.format("<p>Bonjour %s,</p><p>La demande de %s du %s au %s a été %s par le service RH.</p><p>Commentaire: %s</p>", employeeName, type, start, end, decision, commentaire == null ? "" : commentaire);
        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendChefServiceDecisionEmail(String to, String employeeName, String type, String start, String end, String decision, String commentaire) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Décision du chef de service concernant votre demande");
        String content = String.format("<p>Bonjour %s,</p><p>La demande de %s du %s au %s a été %s par le chef de service.</p><p>Commentaire: %s</p>", employeeName, type, start, end, decision, commentaire == null ? "" : commentaire);
        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendDgDecisionEmail(String to, String employeeName, String type, String start, String end, String decision, String commentaire) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Décision du directeur général concernant votre demande");
        String content = String.format("<p>Bonjour %s,</p><p>La demande de %s du %s au %s a été %s par le directeur général.</p><p>Commentaire: %s</p>", employeeName, type, start, end, decision, commentaire == null ? "" : commentaire);
        helper.setText(content, true);
        mailSender.send(message);
    }

    // Additional helper methods and template support can be added here.
}