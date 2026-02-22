package com.laptopMarket.BillionWebsite.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which chat room this message belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    // Who sent this message: "USER" or "ADMIN"
    @Enumerated(EnumType.STRING)
    private SenderType senderType;

    // The sender's ID (user UUID or admin ID)
    private String senderId;

    // The sender's display name
    private String senderName;

    @Column(columnDefinition = "TEXT")
    private String content;

    // For the first message: contains product snapshot (image, name, price)
    private boolean isProductCard; // true = first message with product details

    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}