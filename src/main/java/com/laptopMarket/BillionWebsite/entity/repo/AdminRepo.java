package com.laptopMarket.BillionWebsite.entity.repo;

import com.laptopMarket.BillionWebsite.entity.ShopOwner;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepo extends JpaRepository<ShopOwner, UUID> {
    Optional<ShopOwner> findByEmail(String email);

    boolean existsByEmail(@Email(message = "Enter a valid email") @NotBlank(message = "Email is required") String email);
}
