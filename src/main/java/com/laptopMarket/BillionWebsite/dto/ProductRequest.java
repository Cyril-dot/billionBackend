package com.laptopMarket.BillionWebsite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// ── Used when admin creates or updates a product ─────────────
@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    @NotBlank(message = "Category is required")
    private String category; // laptop, mouse, keyboard, bag, charger, etc.

    private String brand;

    @NotNull(message = "Stock is required")
    private Integer stock;

    // Image is handled separately as MultipartFile in the controller
}