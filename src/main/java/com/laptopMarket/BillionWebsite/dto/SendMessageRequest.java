package com.laptopMarket.BillionWebsite.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Message content cannot be empty")
    private String content;
}