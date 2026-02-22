package com.laptopMarket.BillionWebsite.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomResponse {
    private Long chatRoomId;
    private String title;          // product name
    private Long productId;
    private String productName;
    private String productImage;
    private String productPrice;
    private String customerName;
    private String customerEmail;
    private String adminName;
    private LocalDateTime createdAt;
}