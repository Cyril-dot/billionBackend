package com.laptopMarket.BillionWebsite.controller;

import com.laptopMarket.BillionWebsite.Config.Security.AdminPrincipal;
import com.laptopMarket.BillionWebsite.Config.Security.TokenService;
import com.laptopMarket.BillionWebsite.Config.Security.UserPrincipal;
import com.laptopMarket.BillionWebsite.dto.*;
import com.laptopMarket.BillionWebsite.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService  chatService;
    private final TokenService tokenService;

    // ════════════════════════════════════════════════════════
    // USER — /api/v1/chat/**
    // ════════════════════════════════════════════════════════

    private UserPrincipal userPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        log.debug("Successfully retrieved UserPrincipal for user: {} (ID: {})",
                userPrincipal.getUsername(), userPrincipal.getUserId());

        return userPrincipal;
    }


    private AdminPrincipal adminPrincipal(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AdminPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        AdminPrincipal adminPrincipal = (AdminPrincipal) principal;
        log.debug("Successfully retrieved AdminPrincipal for user: {} (ID: {})",
                adminPrincipal.getUsername(), adminPrincipal.getOwnerId());

        return adminPrincipal;
    }


    @PostMapping("/api/v1/chat/start")
    public ResponseEntity<ChatRoomResponse> startChat(@Valid @RequestBody StartChatRequest request) {
        UserPrincipal userPrincipal = userPrincipal();
        UUID userId = userPrincipal.getUserId();
        log.info("➡️  POST /chat/start - userId: {} | productId: {}", userId, request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.startChat(userId, request));
    }

    /**
     * GET /api/v1/chat/rooms
     * Get all chat rooms started by the logged-in user
     */
    @GetMapping("/api/v1/chat/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getMyChatRooms() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  GET /chat/rooms - userId: {}", userId);
        return ResponseEntity.ok(chatService.getUserChatRooms(userId));
    }

    /**
     * GET /api/v1/chat/rooms/{chatRoomId}/history
     * Load all messages in a chat room ordered by time (oldest first)
     * Used when opening a chat to load previous messages
     */
    @GetMapping("/api/v1/chat/rooms/{chatRoomId}/history")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long chatRoomId) {
        log.info("➡️  GET /chat/rooms/{}/history", chatRoomId);
        return ResponseEntity.ok(chatService.getChatHistory(chatRoomId));
    }

    /**
     * POST /api/v1/chat/rooms/{chatRoomId}/send
     * User sends a message via REST (fallback if WebSocket not available).
     * Admin receives an email notification.
     * Body: { "content": "Is this laptop still available?" }
     */
    @PostMapping("/api/v1/chat/rooms/{chatRoomId}/send")
    public ResponseEntity<ChatMessageResponse> userSendMessage(@PathVariable Long chatRoomId,
            @Valid @RequestBody SendMessageRequest request) {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  POST /chat/rooms/{}/send - userId: {}", chatRoomId, userId);
        return ResponseEntity.ok(chatService.userSendMessage(userId, chatRoomId, request));
    }

    // ════════════════════════════════════════════════════════
    // ADMIN — /api/v1/admin/chat/**
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/admin/chat/rooms
     * Get all chat rooms assigned to this admin (based on their products)
     */
    @GetMapping("/api/v1/admin/chat/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getAdminChatRooms() {
        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();
        log.info("➡️  GET /admin/chat/rooms - adminId: {}", adminId);
        return ResponseEntity.ok(chatService.getAdminChatRooms(adminId));
    }

    /**
     * POST /api/v1/admin/chat/rooms/{chatRoomId}/reply
     * Admin replies to a customer via REST (fallback if WebSocket not available).
     * Customer receives an email notification with the reply.
     * Body: { "content": "Yes it is available! Here are the specs..." }
     */
    @PostMapping("/api/v1/admin/chat/rooms/{chatRoomId}/reply")
    public ResponseEntity<ChatMessageResponse> adminSendMessage(@PathVariable Long chatRoomId,
            @Valid @RequestBody SendMessageRequest request) {
        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();
        log.info("➡️  POST /admin/chat/rooms/{}/reply - adminId: {}", chatRoomId, adminId);
        return ResponseEntity.ok(chatService.adminSendMessage(adminId, chatRoomId, request));
    }

    // ════════════════════════════════════════════════════════
    // WEBSOCKET — STOMP message handlers
    // Frontend connects to: ws://localhost:8080/ws/chat
    // ════════════════════════════════════════════════════════

    /**
     * USER sends a message via WebSocket.
     * Frontend publishes to:   /app/chat/{chatRoomId}/user/send
     * Admin receives on:       /topic/admin/chat/{chatRoomId}
     * Admin also gets an email notification.
     * Payload: { "senderId": "user-uuid", "content": "Hello!" }
     */
    @MessageMapping("/chat/{chatRoomId}/user/send")
    public void userSendMessageWs(
            @DestinationVariable Long chatRoomId,
            @Payload WsMessagePayload payload) {
        log.info("🔌 WS /chat/{}/user/send - sender: {}", chatRoomId, payload.getSenderId());
        SendMessageRequest req = new SendMessageRequest();
        req.setContent(payload.getContent());
        chatService.userSendMessage(
                payload.getSenderId(), chatRoomId, req);
    }

    /**
     * ADMIN replies via WebSocket.
     * Frontend publishes to:  /app/chat/{chatRoomId}/admin/send
     * User receives on:       /topic/user/chat/{chatRoomId}
     * User also gets an email notification.
     * Payload: { "senderId": "1", "content": "Yes it's available!" }
     */
    @MessageMapping("/chat/{chatRoomId}/admin/send")
    public void adminSendMessageWs(
            @DestinationVariable Long chatRoomId,
            @Payload WsMessagePayload payload) {
        log.info("🔌 WS /chat/{}/admin/send - sender: {}", chatRoomId, payload.getSenderId());
        SendMessageRequest req = new SendMessageRequest();
        req.setContent(payload.getContent());
        chatService.adminSendMessage(
                payload.getSenderId(), chatRoomId, req);
    }
}