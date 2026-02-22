package com.laptopMarket.BillionWebsite.entity.repo;

import com.laptopMarket.BillionWebsite.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // Get all chat rooms for a specific user
    List<ChatRoom> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Get all chat rooms for a specific admin
    List<ChatRoom> findByShopOwnerIdOrderByCreatedAtDesc(UUID shopOwnerId);

    // Get all chat rooms about a specific product
    List<ChatRoom> findByProductId(Long productId);

    // Check if a chat room already exists between this user and this product
    Optional<ChatRoom> findByUserIdAndProductId(UUID userId, Long productId);
}