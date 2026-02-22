package com.laptopMarket.BillionWebsite.Config.Security.entity;

import com.laptopMarket.BillionWebsite.entity.ShopOwner;
import com.laptopMarket.BillionWebsite.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Table(name = "admin_tokens")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class AdminRefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "VARCHAR(1000)")
    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false, unique = true)
    private ShopOwner owner;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;
}
