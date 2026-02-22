package com.laptopMarket.BillionWebsite.dto;

import com.laptopMarket.BillionWebsite.entity.Role;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminResponse {

    private UUID id;

    private String name;
    private String email;
    private String phone;
    private String shopAddress;
    private String shopName;
    private Role role;


}

