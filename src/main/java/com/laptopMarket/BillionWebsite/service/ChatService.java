
package com.laptopMarket.BillionWebsite.service;

import com.laptopMarket.BillionWebsite.dto.*;
import com.laptopMarket.BillionWebsite.entity.*;
import com.laptopMarket.BillionWebsite.entity.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository    chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository     productRepository;
    private final UserRepo              userRepository;
    private final AdminRepo             adminRepository;
    private final EmailService          emailService;
    private final SimpMessagingTemplate messagingTemplate; // sends WebSocket messages

    // ─────────────────────────────────────────────────────────
    // START A CHAT ROOM (User initiates about a product)
    // ─────────────────────────────────────────────────────────
    @Transactional
    public ChatRoomResponse startChat(UUID userId, StartChatRequest request) {

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        ShopOwner admin = product.getAddedBy(); // chat goes to the admin who added the product

        // If a chat room already exists for this user + product, return it
        return chatRoomRepository.findByUserIdAndProductId(userId, request.getProductId())
            .map(existing -> {
                System.out.println("💬 Existing chat room found: #" + existing.getId());
                return mapRoomToResponse(existing);
            })
            .orElseGet(() -> {
                // Create new chat room
                ChatRoom room = ChatRoom.builder()
                    .title(product.getName()) // title = product name
                    .product(product)
                    .user(user)
                    .shopOwner(admin)
                    .build();

                ChatRoom savedRoom = chatRoomRepository.save(room);

                // ── First message: auto-generated product card ──────
                // Contains product image, name, price as the opening message
                String productCard = buildProductCardContent(product);

                ChatMessage firstMessage = ChatMessage.builder()
                    .chatRoom(savedRoom)
                    .senderType(SenderType.USER)
                    .senderId(userId.toString())
                    .senderName(user.getFirstName() + " " + user.getLastName())
                    .content(productCard)
                    .isProductCard(true) // marks this as the product card message
                    .build();

                chatMessageRepository.save(firstMessage);

                System.out.println("💬 New chat room created: #" + savedRoom.getId()
                    + " | Product: " + product.getName()
                    + " | User: " + user.getEmail());

                return mapRoomToResponse(savedRoom);
            });
    }

    // ─────────────────────────────────────────────────────────
    // USER SENDS A MESSAGE
    // ─────────────────────────────────────────────────────────
    @Transactional
    public ChatMessageResponse userSendMessage(UUID userId, Long chatRoomId, SendMessageRequest request) {

        ChatRoom room = findRoomById(chatRoomId);

        // Security: only the user who owns this chat room can send here
        if (!room.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This chat does not belong to you");
        }

        User user = room.getUser();

        ChatMessage message = ChatMessage.builder()
            .chatRoom(room)
            .senderType(SenderType.USER)
            .senderId(userId.toString())
            .senderName(user.getFirstName() + " " + user.getLastName())
            .content(request.getContent())
            .isProductCard(false)
            .build();

        ChatMessage saved = chatMessageRepository.save(message);

        ChatMessageResponse response = mapMessageToResponse(saved);

        // ── Push message via WebSocket to admin ─────────────────
        // Admin subscribes to: /topic/admin/chat/{chatRoomId}
        messagingTemplate.convertAndSend(
            "/topic/admin/chat/" + chatRoomId, response
        );

        // ── Send email notification to admin ────────────────────
        emailService.notifyAdminOfUserMessage(
            room.getShopOwner().getEmail(),
            room.getShopOwner().getName(),
            user.getFirstName() + " " + user.getLastName(),
            room.getProduct().getName(),
            request.getContent(),
            chatRoomId
        );

        System.out.println("📨 User [" + user.getEmail() + "] sent message in chat #" + chatRoomId);
        return response;
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN SENDS A REPLY
    // ─────────────────────────────────────────────────────────
    @Transactional
    public ChatMessageResponse adminSendMessage(UUID adminId, Long chatRoomId, SendMessageRequest request) {

        ChatRoom room = findRoomById(chatRoomId);
        ShopOwner admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));

        ChatMessage message = ChatMessage.builder()
            .chatRoom(room)
            .senderType(SenderType.ADMIN)
            .senderId(adminId.toString())
            .senderName(admin.getName())
            .content(request.getContent())
            .isProductCard(false)
            .build();

        ChatMessage saved = chatMessageRepository.save(message);

        ChatMessageResponse response = mapMessageToResponse(saved);

        // ── Push message via WebSocket to the specific user ─────
        // User subscribes to: /topic/user/chat/{chatRoomId}
        messagingTemplate.convertAndSend(
            "/topic/user/chat/" + chatRoomId, response
        );

        // ── Send email notification to the user ─────────────────
        User user = room.getUser();
        emailService.notifyUserOfAdminReply(
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            admin.getName(),
            room.getProduct().getName(),
            request.getContent(),
            chatRoomId
        );

        System.out.println("📨 Admin [" + admin.getName() + "] replied in chat #" + chatRoomId);
        return response;
    }

    // ─────────────────────────────────────────────────────────
    // GET CHAT HISTORY
    // ─────────────────────────────────────────────────────────

    // Load all messages in a room (for when user/admin opens the chat)
    public List<ChatMessageResponse> getChatHistory(Long chatRoomId) {
        return chatMessageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId)
            .stream().map(this::mapMessageToResponse).collect(Collectors.toList());
    }

    // User: get all their chat rooms
    public List<ChatRoomResponse> getUserChatRooms(UUID userId) {
        return chatRoomRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    // Admin: get all chat rooms assigned to them
    public List<ChatRoomResponse> getAdminChatRooms(UUID adminId) {
        return chatRoomRepository.findByShopOwnerIdOrderByCreatedAtDesc(adminId)
            .stream().map(this::mapRoomToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────

    // Builds the first auto-message (product card) content
    private String buildProductCardContent(Product product) {
        return String.format(
            "PRODUCT_CARD::%s::%s::%s::%s",
            product.getName(),
            product.getPrice().toString(),
            product.getDescription() != null ? product.getDescription() : "No description",
            product.getPrimaryImageUrl() != null ? product.getPrimaryImageUrl() : ""
        );
        // Frontend parses "PRODUCT_CARD::" prefix to render a product card UI
        // Format: PRODUCT_CARD::name::price::description::imageUrl
    }

    private ChatRoom findRoomById(Long id) {
        return chatRoomRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Chat room not found with id: " + id));
    }

    private ChatRoomResponse mapRoomToResponse(ChatRoom room) {
        return ChatRoomResponse.builder()
            .chatRoomId(room.getId())
            .title(room.getTitle())
            .productId(room.getProduct().getId())
            .productName(room.getProduct().getName())
            .productImage(room.getProduct().getPrimaryImageUrl())
            .productPrice("$" + room.getProduct().getPrice())
            .customerName(room.getUser().getFirstName() + " " + room.getUser().getLastName())
            .customerEmail(room.getUser().getEmail())
            .adminName(room.getShopOwner().getName())
            .createdAt(room.getCreatedAt())
            .build();
    }

    private ChatMessageResponse mapMessageToResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
            .messageId(msg.getId())
            .chatRoomId(msg.getChatRoom().getId())
            .senderType(msg.getSenderType().name())
            .senderName(msg.getSenderName())
            .content(msg.getContent())
            .isProductCard(msg.isProductCard())
            .sentAt(msg.getSentAt())
            .build();
    }
}