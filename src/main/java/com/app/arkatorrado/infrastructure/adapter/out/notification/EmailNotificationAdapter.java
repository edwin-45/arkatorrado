package com.app.arkatorrado.infrastructure.adapter.out.notification;

import com.app.arkatorrado.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Email notification adapter
 * Implements email sending functionality for e-commerce notifications
 */
@Component
@Primary
public class EmailNotificationAdapter implements NotificationPort {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationAdapter.class);
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        // In a real implementation, this would integrate with an email service like:
        // - SendGrid
        // - Amazon SES
        // - SMTP server
        // - etc.
        
        logger.info("📧 EMAIL SENT =====================================");
        logger.info("To: {}", to);
        logger.info("Subject: {}", subject);
        logger.info("Body: {}", body);
        logger.info("====================================================");
        
        // Simulate email sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void sendAbandonedCartReminder(String customerEmail, String customerName, Long cartId) {
        String subject = "🛒 ¡No olvides tu carrito en ARKA!";
        String body = String.format(
            "Hola %s,\n\n" +
            "Notamos que tienes productos en tu carrito (ID: %d) que están esperándote.\n\n" +
            "¡No pierdas esta oportunidad! Completa tu compra ahora:\n" +
            "👉 https://arka.com/cart/%d\n\n" +
            "¿Necesitas ayuda? Contáctanos.\n\n" +
            "Saludos,\n" +
            "El equipo de ARKA",
            customerName, cartId, cartId
        );
        
        sendEmail(customerEmail, subject, body);
    }
    
    @Override
    public void sendOrderConfirmation(String customerEmail, String customerName, Long orderId) {
        String subject = "✅ Confirmación de pedido #" + orderId;
        String body = String.format(
            "Hola %s,\n\n" +
            "¡Gracias por tu compra!\n\n" +
            "Tu pedido #%d ha sido confirmado y está siendo procesado.\n\n" +
            "Puedes seguir el estado de tu pedido en:\n" +
            "👉 https://arka.com/orders/%d\n\n" +
            "Te notificaremos cuando tu pedido esté en camino.\n\n" +
            "Saludos,\n" +
            "El equipo de ARKA",
            customerName, orderId, orderId
        );
        
        sendEmail(customerEmail, subject, body);
    }
    
    @Override
    public void sendStockAlert(String productName, Integer currentStock) {
        String adminEmail = "admin@arka.com";
        String subject = "⚠️ Alerta de Stock Bajo - " + productName;
        String body = String.format(
            "ALERTA DE INVENTARIO\n\n" +
            "El producto '%s' tiene stock bajo.\n" +
            "Stock actual: %d unidades\n\n" +
            "Considera reabastecer el inventario.\n\n" +
            "Sistema de Inventario ARKA",
            productName, currentStock
        );
        
        sendEmail(adminEmail, subject, body);
    }
}
