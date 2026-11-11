package com.shinchoku.shinchokuapp.service;

import com.shinchoku.shinchokuapp.api.ApiException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Delivers invitation tokens through SMTP, or writes local-only development mail files. Tokens are
 * never included in invitation listing responses.
 */
@Service
public class InvitationDelivery {

  private final ObjectProvider<JavaMailSender> mailSenderProvider;

  private final boolean mailEnabled, localProfileActive;
  private final String senderAddress, frontendUrl;

  public InvitationDelivery(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      Environment environment,
      @Value("${app.mail-enabled}") boolean mailEnabled,
      @Value("${app.mail-from}") String senderAddress,
      @Value("${app.frontend-url}") String frontendUrl) {
    this.mailSenderProvider = mailSenderProvider;
    this.mailEnabled = mailEnabled;
    this.senderAddress = senderAddress;
    this.frontendUrl = frontendUrl;
    this.localProfileActive = Arrays.asList(environment.getActiveProfiles()).contains("local");
  }

  /** Fails before creating an invitation when production email delivery is unavailable. */
  public void ensureAvailable() {
    if (!localProfileActive && (!mailEnabled || mailSenderProvider.getIfAvailable() == null)) {
      throw new ApiException(
          503, "MAIL_NOT_CONFIGURED", "Invitation email delivery has not been configured");
    }
  }

  /**
   * Local development writes an ignored mail file; other profiles send through the configured SMTP
   * sender.
   */
  public void send(String recipientEmail, String invitationToken) {
    String emailBody =
        "You have been invited to Shinchoku. Accept using this token within seven days: "
            + invitationToken;
    if (localProfileActive) {
      try {
        var mailDirectory = Path.of("local-mail");
        Files.createDirectories(mailDirectory);
        Files.writeString(
            mailDirectory.resolve(UUID.randomUUID() + ".eml"),
            "To: " + recipientEmail + "\nSubject: Shinchoku invitation\n\n" + emailBody);
      } catch (java.io.IOException exception) {
        throw new IllegalStateException("Cannot write local invitation", exception);
      }
      return;
    }
    var emailMessage = new SimpleMailMessage();
    emailMessage.setFrom(senderAddress);
    emailMessage.setTo(recipientEmail);
    emailMessage.setSubject("Shinchoku invitation");
    emailMessage.setText(emailBody);
    mailSenderProvider.getObject().send(emailMessage);
  }
}
