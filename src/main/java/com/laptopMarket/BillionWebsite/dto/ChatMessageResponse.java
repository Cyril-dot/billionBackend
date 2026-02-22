package com.laptopMarket.BillionWebsite.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private Long chatRoomId;
    private String senderType;   // "USER" or "ADMIN"
    private String senderName;
    private String content;
    private boolean isProductCard;
    private LocalDateTime sentAt;
}