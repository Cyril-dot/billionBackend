package com.laptopMarket.BillionWebsite.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A ChatRoom is created when a user wants to chat about a specific product.
 * Title = product name, linked to both the user and the product.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chat title = product name (set automatically when room is created)
    private String title;

    // The product this chat is about
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // The customer who initiated the chat
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The admin/shop owner involved in this chat
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_owner_id", nullable = false)
    private ShopOwner shopOwner;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}