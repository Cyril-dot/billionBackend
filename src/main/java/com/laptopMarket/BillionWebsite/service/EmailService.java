package com.laptopMarket.BillionWebsite.service;

import com.laptopMarket.BillionWebsite.entity.Product;
import com.laptopMarket.BillionWebsite.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String SHOP_NAME   = "Billions Laptops";
    private static final String FOOTER_TEXT = "Billions Laptops — Your Trusted Laptop & Accessories Store";
    private static final String HEADER_BG   = "#0a235a";

    // ─────────────────────────────────────────────────────────
    // CHAT NOTIFICATIONS
    // ─────────────────────────────────────────────────────────

    /**
     * Email sent to ADMIN when a USER sends a message.
     * Contains the customer name, product name, and message content.
     */
    @Async
    public void notifyAdminOfUserMessage(String adminEmail, String adminName,
                                         String customerName, String productName,
                                         String messageContent, Long chatRoomId) {
        String subject = "Billions Laptops | 💬 New message from " + customerName + " about \"" + productName + "\"";

        String body = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
              <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
                <div style="background:%s; padding:20px; color:white;">
                  <h2 style="margin:0;">💬 New Customer Message</h2>
                  <p style="margin:4px 0 0; opacity:0.7; font-size:13px;">%s</p>
                </div>
                <div style="padding:24px;">
                  <p>Hi <strong>%s</strong>,</p>
                  <p><strong>%s</strong> sent you a message about <strong>"%s"</strong>:</p>
                  <div style="background:#f5f5f5; padding:16px; border-left:4px solid #0a235a; border-radius:4px; margin:16px 0;">
                    <p style="margin:0; font-size:15px;">"%s"</p>
                  </div>
                  <a href="http://localhost:8080/chat/%d"
                     style="background:#0a235a; color:white; padding:12px 24px; border-radius:6px;
                            text-decoration:none; display:inline-block; margin-top:8px;">
                    Reply to Customer
                  </a>
                </div>
                <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">
                  %s
                </div>
              </div>
            </body></html>
            """.formatted(HEADER_BG, SHOP_NAME, adminName, customerName, productName, messageContent, chatRoomId, FOOTER_TEXT);

        sendHtmlEmail(adminEmail, subject, body);
    }

    /**
     * Email sent to USER when ADMIN replies to their message.
     * Contains the admin reply and a link back to the chat.
     */
    @Async
    public void notifyUserOfAdminReply(String userEmail, String userName,
                                       String adminName, String productName,
                                       String messageContent, Long chatRoomId) {
        String subject = "Billions Laptops | 💬 " + adminName + " replied about \"" + productName + "\"";

        String body = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
              <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
                <div style="background:%s; padding:20px; color:white;">
                  <h2 style="margin:0;">💬 You have a new reply!</h2>
                  <p style="margin:4px 0 0; opacity:0.7; font-size:13px;">%s</p>
                </div>
                <div style="padding:24px;">
                  <p>Hi <strong>%s</strong>,</p>
                  <p><strong>%s</strong> replied to your enquiry about <strong>"%s"</strong>:</p>
                  <div style="background:#f5f5f5; padding:16px; border-left:4px solid #28a745; border-radius:4px; margin:16px 0;">
                    <p style="margin:0; font-size:15px;">"%s"</p>
                  </div>
                  <a href="http://localhost:8080/chat/%d"
                     style="background:#28a745; color:white; padding:12px 24px; border-radius:6px;
                            text-decoration:none; display:inline-block; margin-top:8px;">
                    View Conversation
                  </a>
                </div>
                <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">
                  %s
                </div>
              </div>
            </body></html>
            """.formatted(HEADER_BG, SHOP_NAME, userName, adminName, productName, messageContent, chatRoomId, FOOTER_TEXT);

        sendHtmlEmail(userEmail, subject, body);
    }

    // ─────────────────────────────────────────────────────────
    // NEW PRODUCT ANNOUNCEMENT — sent to ALL users
    // ─────────────────────────────────────────────────────────

    /**
     * When admin uploads a new product, send an email to ALL users.
     * Email contains product image, name, description, and price.
     */
    @Async
    public void announceNewProductToAllUsers(List<User> allUsers, Product product, String adminShopName) {
        String subject = "Billions Laptops | 🆕 New arrival: " + product.getName() + " just dropped!";

        String body = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
              <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
                <div style="background:%s; padding:20px; color:white;">
                  <h2 style="margin:0;">🆕 New Product Just Added!</h2>
                  <p style="margin:4px 0 0; opacity:0.7; font-size:13px;">%s</p>
                </div>
                %s
                <div style="padding:24px;">
                  <h2 style="margin:0 0 8px;">%s</h2>
                  <p style="color:#0a235a; font-size:22px; font-weight:bold; margin:0 0 16px;">₵%s</p>
                  <p style="color:#555; line-height:1.6;">%s</p>
                  <p style="margin:4px 0;"><strong>Category:</strong> %s</p>
                  <p style="margin:4px 0;"><strong>Brand:</strong> %s</p>
                  <p style="margin:4px 0;"><strong>In Stock:</strong> %d units available</p>
                  <a href="http://localhost:8080/products/%d"
                     style="background:#0a235a; color:white; padding:12px 28px; border-radius:6px;
                            text-decoration:none; display:inline-block; margin-top:20px; font-size:15px;">
                    View Product
                  </a>
                </div>
                <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">
                  %s
                </div>
              </div>
            </body></html>
            """.formatted(
                HEADER_BG,
                SHOP_NAME,
                product.getPrimaryImageUrl() != null
                        ? "<img src='" + product.getPrimaryImageUrl() + "' style='width:100%; max-height:300px; object-fit:cover;'/>"
                        : "",
                product.getName(),
                product.getPrice(),
                product.getDescription() != null ? product.getDescription() : "Check out our latest product!",
                product.getCategory(),
                product.getBrand() != null ? product.getBrand() : "N/A",
                product.getStock(),
                product.getId(),
                FOOTER_TEXT
        );

        // Send to each user asynchronously so it doesn't block the product upload
        allUsers.forEach(user -> sendHtmlEmail(user.getEmail(), subject, body));

        System.out.println("📧 Product announcement sent to " + allUsers.size() + " users for: " + product.getName());
    }

    // ─────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
            System.out.println("📧 Email sent to: " + to + " | Subject: " + subject);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}