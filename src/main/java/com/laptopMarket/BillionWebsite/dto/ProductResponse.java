package com.laptopMarket.BillionWebsite.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ── Returned when viewing products ───────────────────────────
@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String brand;
    private Integer stock;
    private List<String> imageUrls;      // all images in display order
    private String imageUrl;
    private String addedByAdmin; // shop owner name who added this product
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}