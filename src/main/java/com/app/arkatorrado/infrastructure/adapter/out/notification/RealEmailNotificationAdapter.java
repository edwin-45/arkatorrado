package com.app.arkatorrado.infrastructure.adapter.out.notification;

import com.app.arkatorrado.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real SMTP Email notification adapter
 * Sends actual emails using JavaMailSender
 */
@Component("realEmailNotificationAdapter")
public class RealEmailNotificationAdapter implements NotificationPort {
    
    private static final Logger logger = LoggerFactory.getLogger(RealEmailNotificationAdapter.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${arka.mail.from:noreply@arka.com}")
    private String fromEmail;
    
    @Value("${arka.mail.enabled:false}")
    private boolean emailEnabled;
    
    public RealEmailNotificationAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            logger.info("📧 EMAIL DISABLED - Would send to: {} | Subject: {}", to, subject);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            logger.info("✅ EMAIL SENT successfully to: {} | Subject: {}", to, subject);
            
            // Save to MongoDB (if available)
            saveNotificationHistory("EMAIL", to, subject, body, "SENT", null);
            
        } catch (Exception e) {
            logger.error("❌ ERROR sending email to {}: {}", to, e.getMessage());
            saveNotificationHistory("EMAIL", to, subject, body, "FAILED", e.getMessage());
            
            // Fallback to logging
            logEmailFallback(to, subject, body);
        }
    }
    
    @Override
    public void sendAbandonedCartReminder(String customerEmail, String customerName, Long cartId) {
        String subject = "🛒 ¡No olvides tu carrito en ARKA!";
        String body = createAbandonedCartEmailBody(customerName, cartId);
        
        sendEmail(customerEmail, subject, body);
        saveNotificationHistory("EMAIL", customerEmail, subject, body, "SENT", "CART", cartId.toString());
    }
    
    @Override
    public void sendOrderConfirmation(String customerEmail, String customerName, Long orderId) {
        String subject = "✅ Confirmación de pedido #" + orderId + " - ARKA";
        String body = createOrderConfirmationEmailBody(customerName, orderId);
        
        sendEmail(customerEmail, subject, body);
        saveNotificationHistory("EMAIL", customerEmail, subject, body, "SENT", "ORDER", orderId.toString());
    }
    
    @Override
    public void sendStockAlert(String productName, Integer currentStock) {
        String adminEmail = "admin@arka.com";
        String subject = "⚠️ Alerta de Stock Bajo - " + productName;
        String body = createStockAlertEmailBody(productName, currentStock);
        
        sendEmail(adminEmail, subject, body);
        saveNotificationHistory("EMAIL", adminEmail, subject, body, "SENT", "PRODUCT", productName);
    }
    
    private String createAbandonedCartEmailBody(String customerName, Long cartId) {
        return String.format("""
            Hola %s,
            
            Notamos que tienes productos en tu carrito (ID: %d) que están esperándote.
            
            ¡No pierdas esta oportunidad! Completa tu compra ahora:
            👉 https://arka.com/cart/%d
            
            ¿Necesitas ayuda? Contáctanos en soporte@arka.com
            
            Saludos,
            El equipo de ARKA
            
            ---
            Este es un email automático, por favor no respondas a este mensaje.
            """, customerName, cartId, cartId);
    }
    
    private String createOrderConfirmationEmailBody(String customerName, Long orderId) {
        return String.format("""
            Hola %s,
            
            ¡Gracias por tu compra!
            
            Tu pedido #%d ha sido confirmado y está siendo procesado.
            
            Puedes seguir el estado de tu pedido en:
            👉 https://arka.com/orders/%d
            
            Te notificaremos cuando tu pedido esté en camino.
            
            Saludos,
            El equipo de ARKA
            
            ---
            Para cualquier consulta, contacta: soporte@arka.com
            """, customerName, orderId, orderId);
    }
    
    private String createStockAlertEmailBody(String productName, Integer currentStock) {
        return String.format("""
            ALERTA DE INVENTARIO
            
            El producto '%s' tiene stock bajo.
            Stock actual: %d unidades
            
            Considera reabastecer el inventario lo antes posible.
            
            Sistema de Inventario ARKA
            """, productName, currentStock);
    }
    
    private void logEmailFallback(String to, String subject, String body) {
        logger.info("📧 EMAIL FALLBACK =====================================");
        logger.info("To: {}", to);
        logger.info("Subject: {}", subject);
        logger.info("Body: {}", body);
        logger.info("=====================================================");
    }
    
    private void saveNotificationHistory(String type, String recipient, String subject, 
                                       String content, String status, String error) {
        // TODO: Implement MongoDB saving when dependencies are loaded
        logger.debug("💾 Would save notification to MongoDB: {} to {}", type, recipient);
    }
    
    private void saveNotificationHistory(String type, String recipient, String subject, 
                                       String content, String status, String entityType, String entityId) {
        // TODO: Implement MongoDB saving when dependencies are loaded  
        logger.debug("💾 Would save notification to MongoDB: {} to {} for {} {}", 
                    type, recipient, entityType, entityId);
    }
}
