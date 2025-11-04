package com.store.store.service.impl;

import com.store.store.entity.Customer;
import com.store.store.service.ISecurityAlertService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of the ISecurityAlertService interface for managing security alerts.
 * Provides functionality to send notifications about suspicious activities,
 * new device logins, and token revocations to users.
 *
 * This class uses asynchronous processing (@Async) for email notifications to improve
 * application performance and user experience.
 * Emails are sent with HTML content for better visual presentation, including inline CSS
 * for enhanced compatibility with various email clients.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAlertServiceImpl implements ISecurityAlertService {

    // Service Spring Mail pour l'envoi d'emails
    private final JavaMailSender mailSender;

    /**
     * Sends a security alert notification to a customer for a potential account compromise incident.
     * This method operates asynchronously and utilizes HTML for rich email formatting.
     *
     * @param customer The customer to whom the notification will be sent.
     *                 Must contain a valid email address.
     * @param ipAddress The IP address associated with the suspicious activity.
     * @param userAgent The User-Agent string of the device involved in the suspected incident.
     * @param incidentType The type or description of the detected security incident.
     */
    @Async
    @Override
    public void notifyPossibleAccountCompromise(Customer customer, String ipAddress, String userAgent, String incidentType) {

        try {
            // Validation: Vérifier que le customer a un email valide
            if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
                log.error("Cannot send security alert: customer email is null or blank");
                return;
            }

            log.info("Sending security alert email to: {}", customer.getEmail());

            // Formater la date/heure actuelle pour l'email
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));

            // Extraire informations device lisibles
            String deviceInfo = extractDeviceInfo(userAgent);

            // Créer le message HTML avec CSS inline pour compatibilité email
            String htmlContent = buildSecurityAlertHtml(customer.getName(), incidentType, timestamp, ipAddress, deviceInfo);

            // Créer le MimeMessage pour envoyer du HTML
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(customer.getEmail());
            helper.setSubject("🚨 Alerte Sécurité - Activité Suspecte Détectée");
            helper.setText(htmlContent, true);  // true = HTML content

            // Optionnel: personnalisé
            // helper.setFrom("security@eazystore.com");

            // Envoyer l'email
            mailSender.send(message);

            log.info("Security alert email sent successfully to: {}", customer.getEmail());

        } catch (MessagingException e) {
            // Erreur spécifique à la création du message
            log.error("Failed to create security alert email for {}: {}", customer.getEmail(), e.getMessage(), e);
        } catch (Exception e) {
            // Logger l'erreur mais ne pas propager l'exception
            // (l'envoi d'email ne doit pas faire échouer l'opération principale)
            log.error("Failed to send security alert email to {}: {}", customer.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Sends a notification to a customer when a new device is used to log into their account.
     * The notification includes details such as the IP address, timestamp, and device information.
     * This method is executed asynchronously.
     *
     * @param customer The customer to whom the notification will be sent. Must contain a valid email address.
     * @param ipAddress The IP address of the device used during the login.
     * @param userAgent The User-Agent string describing the browser or device used for the login.
     */
    @Async
    @Override
    public void notifyNewDeviceLogin(
            Customer customer,
            String ipAddress,
            String userAgent) {

        try {
            if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
                log.error("Cannot send new device notification: customer email is null or blank");
                return;
            }

            log.info("Sending new device notification to: {}", customer.getEmail());

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));

            String deviceInfo = extractDeviceInfo(userAgent);

            // Créer le contenu HTML
            String htmlContent = buildNewDeviceLoginHtml(customer.getName(), timestamp, ipAddress, deviceInfo);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(customer.getEmail());
            helper.setSubject("Nouvelle Connexion Détectée");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("New device notification sent successfully to: {}", customer.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to create new device notification for {}: {}", customer.getEmail(), e.getMessage(), e);
        } catch (Exception e) {
            assert customer != null;
            log.error("Failed to send new device notification to {}: {}", customer.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Sends a notification to a customer indicating that all active tokens have been revoked.
     * The notification is sent via email and includes details about the revocation and its reason.
     * This method is executed asynchronously.
     *
     * @param customer The customer to whom the notification will be sent. Must contain a valid email address.
     * @param reason The reason for the revocation of all tokens.
     */
    @Async
    @Override
    public void notifyAllTokensRevoked(Customer customer, String reason) {

        try {
            if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
                log.error("Cannot send tokens revoked notification: customer email is null or blank");
                return;
            }

            log.info("Sending tokens revoked notification to: {}", customer.getEmail());

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"));

            // Créer le contenu HTML
            String htmlContent = buildTokensRevokedHtml(customer.getName(), reason, timestamp);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(customer.getEmail());
            helper.setSubject("🔐 Déconnexion de Tous Vos Appareils");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Tokens revoked notification sent successfully to: {}", customer.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to create tokens revoked notification for {}: {}",
                    customer.getEmail(), e.getMessage(), e);
        } catch (Exception e) {
            assert customer != null;
            log.error("Failed to send tokens revoked notification to {}: {}", customer.getEmail(), e.getMessage(), e);
        }
    }

    // MÉTHODES PRIVÉES - HTML TEMPLATES

    /**
     * Generates an HTML string for a security alert notification sent to a customer.
     *
     * @param customerName the name of the customer receiving the alert
     * @param incidentType the type of security incident that occurred
     * @param timestamp the date and time when the incident was detected
     * @param ipAddress the IP address involved in the incident
     * @param deviceInfo information about the device associated with the incident
     * @return a string containing the HTML content of the security alert message
     */
    private String buildSecurityAlertHtml(
            String customerName,
            String incidentType,
            String timestamp,
            String ipAddress,
            String deviceInfo) {

        return String.format("""
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Alerte Sécurité</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                    <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <!-- Container principal -->
                                <table role="presentation" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                    
                                    <!-- Header avec alerte -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%); padding: 30px; text-align: center; border-radius: 8px 8px 0 0;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 28px;">
                                                🚨 Alerte Sécurité
                                            </h1>
                                        </td>
                                    </tr>
                                    
                                    <!-- Corps du message -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <p style="margin: 0 0 20px 0; font-size: 16px; color: #333333;">
                                                Bonjour <strong>%s</strong>,
                                            </p>
                                            
                                            <p style="margin: 0 0 30px 0; font-size: 16px; color: #333333; line-height: 1.6;">
                                                Une <strong>activité suspecte</strong> a été détectée sur votre compte.
                                            </p>
                                            
                                            <!-- Box détails incident -->
                                            <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 20px; margin-bottom: 30px; border-radius: 4px;">
                                                <h2 style="margin: 0 0 15px 0; font-size: 18px; color: #856404;">
                                                    📋 Détails de l'Incident
                                                </h2>
                                                <table style="width: 100%%; border-collapse: collapse;">
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #856404; font-weight: bold;">Type d'incident:</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #856404;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #856404; font-weight: bold;">Date et heure:</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #856404;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #856404; font-weight: bold;">Adresse IP:</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #856404;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #856404; font-weight: bold;">Appareil:</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #856404;">%s</td>
                                                    </tr>
                                                </table>
                                            </div>
                                            
                                            <!-- Mesures prises -->
                                            <div style="background-color: #d4edda; border-left: 4px solid #28a745; padding: 20px; margin-bottom: 30px; border-radius: 4px;">
                                                <h2 style="margin: 0 0 15px 0; font-size: 18px; color: #155724;">
                                                    ✅ Mesures de Sécurité Prises
                                                </h2>
                                                <ul style="margin: 0; padding-left: 20px; color: #155724; font-size: 14px; line-height: 1.8;">
                                                    <li>Tous vos tokens d'authentification ont été révoqués</li>
                                                    <li>Vous devez vous reconnecter pour accéder à votre compte</li>
                                                </ul>
                                            </div>
                                            
                                            <!-- Instructions -->
                                            <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #333333;">
                                                Si c'était vous:
                                            </h3>
                                            <p style="margin: 0 0 20px 0; font-size: 14px; color: #666666; line-height: 1.6;">
                                                Reconnectez-vous simplement à votre compte.
                                            </p>
                                            
                                            <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #dc3545;">
                                                ⚠️ Si ce n'était PAS vous:
                                            </h3>
                                            <ol style="margin: 0 0 30px 0; padding-left: 20px; font-size: 14px; color: #666666; line-height: 1.8;">
                                                <li>Changez votre mot de passe <strong>IMMÉDIATEMENT</strong></li>
                                                <li>Vérifiez vos informations de compte</li>
                                                <li>Contactez notre support si nécessaire</li>
                                            </ol>
                                            
                                            <!-- Recommandations -->
                                            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 4px; margin-bottom: 20px;">
                                                <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #333333;">
                                                    💡 Recommandations
                                                </h3>
                                                <ul style="margin: 0; padding-left: 20px; font-size: 14px; color: #666666; line-height: 1.8;">
                                                    <li>Utilisez un mot de passe fort et unique</li>
                                                    <li>Activez l'authentification à deux facteurs (si disponible)</li>
                                                    <li>Ne partagez jamais vos identifiants</li>
                                                </ul>
                                            </div>
                                            
                                            <!-- Contact support -->
                                            <p style="margin: 0; font-size: 14px; color: #666666; text-align: center; line-height: 1.6;">
                                                En cas de questions, contactez-nous:<br>
                                                <a href="mailto:support@eazystore.com" style="color: #007bff; text-decoration: none;">support@eazystore.com</a>
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px;">
                                            <p style="margin: 0; font-size: 14px; color: #666666;">
                                                Cordialement,<br>
                                                <strong>L'équipe Eazy Store Security</strong>
                                            </p>
                                            <p style="margin: 15px 0 0 0; font-size: 12px; color: #999999;">
                                                Cet email a été envoyé automatiquement. Ne pas répondre.
                                            </p>
                                        </td>
                                    </tr>
                                    
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                customerName,
                incidentType,
                timestamp,
                ipAddress,
                deviceInfo
        );
    }

    /**
     * Builds the HTML content for the "new device login" notification email.
     *
     * @param customerName the name of the customer to address in the email
     * @param timestamp the date and time of the detected login
     * @param ipAddress the IP address from which the login was made
     * @param deviceInfo details about the device used for the login
     * @return a string representing the formatted HTML content for the email
     */
    private String buildNewDeviceLoginHtml(
            String customerName,
            String timestamp,
            String ipAddress,
            String deviceInfo) {

        return String.format("""
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                    <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <table role="presentation" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                    
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #17a2b8 0%%, #138496 100%%); padding: 30px; text-align: center; border-radius: 8px 8px 0 0;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 28px;">
                                                ℹ️ Nouvelle Connexion
                                            </h1>
                                        </td>
                                    </tr>
                                    
                                    <!-- Corps -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <p style="margin: 0 0 20px 0; font-size: 16px; color: #333333;">
                                                Bonjour <strong>%s</strong>,
                                            </p>
                                            
                                            <p style="margin: 0 0 30px 0; font-size: 16px; color: #333333; line-height: 1.6;">
                                                Une nouvelle connexion à votre compte a été détectée.
                                            </p>
                                            
                                            <!-- Détails connexion -->
                                            <div style="background-color: #d1ecf1; border-left: 4px solid #17a2b8; padding: 20px; margin-bottom: 30px; border-radius: 4px;">
                                                <h2 style="margin: 0 0 15px 0; font-size: 18px; color: #0c5460;">
                                                    📋 Détails de la Connexion
                                                </h2>
                                                <table style="width: 100%%; border-collapse: collapse;">
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #0c5460; font-weight: bold;">Date et heure:</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #0c5460;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #0c5460; font-weight: bold;">Adresse IP:</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #0c5460;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #0c5460; font-weight: bold;">Appareil:</td>
                                                        <td style="padding: 8px 0; font-size: 14px; color: #0c5460;">%s</td>
                                                    </tr>
                                                </table>
                                            </div>
                                            
                                            <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #28a745;">
                                                ✅ Si c'était vous:
                                            </h3>
                                            <p style="margin: 0 0 20px 0; font-size: 14px; color: #666666;">
                                                Aucune action n'est requise. Vous pouvez ignorer cet email.
                                            </p>
                                            
                                            <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #dc3545;">
                                                ⚠️ Si ce n'était PAS vous:
                                            </h3>
                                            <ol style="margin: 0 0 20px 0; padding-left: 20px; font-size: 14px; color: #666666; line-height: 1.8;">
                                                <li>Changez votre mot de passe immédiatement</li>
                                                <li>Contactez notre support: <a href="mailto:support@eazystore.com" style="color: #007bff;">support@eazystore.com</a></li>
                                            </ol>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px;">
                                            <p style="margin: 0; font-size: 14px; color: #666666;">
                                                Cordialement,<br>
                                                <strong>L'équipe Eazy Store</strong>
                                            </p>
                                            <p style="margin: 15px 0 0 0; font-size: 12px; color: #999999;">
                                                Cet email a été envoyé automatiquement. Ne pas répondre.
                                            </p>
                                        </td>
                                    </tr>
                                    
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                customerName,
                timestamp,
                ipAddress,
                deviceInfo
        );
    }

    /**
     * Builds an HTML string for a notification email indicating that tokens have been revoked,
     * including the customer name, reason, and timestamp.
     *
     * @param customerName the name of the customer to include in the email.
     * @param reason the reason for the token revocation.
     * @param timestamp the timestamp of the revocation event.
     * @return an HTML string formatted for a token revocation email.
     */
    private String buildTokensRevokedHtml(
            String customerName,
            String reason,
            String timestamp) {

        return String.format("""
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                    <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <table role="presentation" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                    
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #6c757d 0%%, #5a6268 100%%); padding: 30px; text-align: center; border-radius: 8px 8px 0 0;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 28px;">
                                                🔐 Déconnexion Sécurisée
                                            </h1>
                                        </td>
                                    </tr>
                                    
                                    <!-- Corps -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <p style="margin: 0 0 20px 0; font-size: 16px; color: #333333;">
                                                Bonjour <strong>%s</strong>,
                                            </p>
                                            
                                            <p style="margin: 0 0 30px 0; font-size: 16px; color: #333333; line-height: 1.6;">
                                                Pour votre sécurité, vous avez été <strong>déconnecté de tous vos appareils</strong>.
                                            </p>
                                            
                                            <!-- Raison -->
                                            <div style="background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 20px; margin-bottom: 30px; border-radius: 4px;">
                                                <h2 style="margin: 0 0 10px 0; font-size: 18px; color: #721c24;">
                                                    Raison
                                                </h2>
                                                <p style="margin: 0; font-size: 14px; color: #721c24;">
                                                    %s
                                                </p>
                                                <p style="margin: 10px 0 0 0; font-size: 14px; color: #721c24;">
                                                    <strong>Date:</strong> %s
                                                </p>
                                            </div>
                                            
                                            <!-- Instructions -->
                                            <h3 style="margin: 0 0 15px 0; font-size: 18px; color: #333333;">
                                                Que faire maintenant?
                                            </h3>
                                            <ol style="margin: 0 0 30px 0; padding-left: 20px; font-size: 14px; color: #666666; line-height: 1.8;">
                                                <li>Reconnectez-vous avec vos identifiants habituels</li>
                                                <li>Si vous ne parvenez pas à vous connecter, réinitialisez votre mot de passe</li>
                                                <li>Vérifiez vos informations de compte</li>
                                            </ol>
                                            
                                            <!-- Support -->
                                            <div style="background-color: #d1ecf1; padding: 20px; border-radius: 4px;">
                                                <h3 style="margin: 0 0 10px 0; font-size: 16px; color: #0c5460;">
                                                    📞 Besoin d'aide?
                                                </h3>
                                                <p style="margin: 0; font-size: 14px; color: #0c5460; line-height: 1.6;">
                                                    Notre équipe support est disponible:<br>
                                                    📧 Email: <a href="mailto:support@eazystore.com" style="color: #007bff;">support@eazystore.com</a><br>
                                                    📞 Téléphone: +33 1 23 45 67 89
                                                </p>
                                            </div>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px;">
                                            <p style="margin: 0; font-size: 14px; color: #666666;">
                                                Cordialement,<br>
                                                <strong>L'équipe Eazy Store Security</strong>
                                            </p>
                                            <p style="margin: 15px 0 0 0; font-size: 12px; color: #999999;">
                                                Cet email a été envoyé automatiquement. Ne pas répondre.
                                            </p>
                                        </td>
                                    </tr>
                                    
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                customerName,
                reason,
                timestamp
        );
    }

    /**
     * Extracts information about the browser and operating system from a given User-Agent string.
     * If the User-Agent string is null or empty, it returns a default message indicating an unknown device.
     *
     * @param userAgent the User-Agent string containing details of the browser and operating system
     * @return a string in the format "Browser on OS", or "Appareil inconnu" for unknown devices
     */
    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Appareil inconnu";
        }

        // Extraire le navigateur
        String browser = "Navigateur inconnu";
        if (userAgent.contains("Chrome")) browser = "Chrome";
        else if (userAgent.contains("Firefox")) browser = "Firefox";
        else if (userAgent.contains("Safari")) browser = "Safari";
        else if (userAgent.contains("Edge")) browser = "Edge";
        else if (userAgent.contains("Opera")) browser = "Opera";

        // Extraire le système d'exploitation
        String os = "OS inconnu";
        if (userAgent.contains("Windows")) os = "Windows";
        else if (userAgent.contains("Mac OS")) os = "macOS";
        else if (userAgent.contains("Linux")) os = "Linux";
        else if (userAgent.contains("Android")) os = "Android";
        else if (userAgent.contains("iOS") || userAgent.contains("iPhone")) os = "iOS";

        return browser + " sur " + os;
    }
}