package com.laptopMarket.BillionWebsite.controller;

import com.laptopMarket.BillionWebsite.Config.Security.TokenService;
import com.laptopMarket.BillionWebsite.Config.Security.UserPrincipal;
import com.laptopMarket.BillionWebsite.dto.OrderResponse;
import com.laptopMarket.BillionWebsite.dto.PlaceOrderRequest;
import com.laptopMarket.BillionWebsite.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SHOP_OWNER', 'USER')")
public class OrderController {

    private final OrderService orderService;

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



    /**
     * POST /api/v1/orders
     * Place an order from the current cart.
     * Cart must not be empty. Stock is deducted automatically.
     * Cart is cleared after order is placed.
     * Body: { "deliveryAddress": "123 Main St, Accra" }
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  POST /orders - userId: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userId, request));
    }

    /**
     * GET /api/v1/orders
     * Get all orders placed by the logged-in user, newest first
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  GET /orders - userId: {}", userId);
        return ResponseEntity.ok(orderService.getMyOrders(userId));
    }

    /**
     * GET /api/v1/orders/{orderId}
     * Get a single order by ID.
     * Returns 403 if the order does not belong to this user.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  GET /orders/{} - userId: {}", orderId, userId);
        return ResponseEntity.ok(orderService.getMyOrderById(orderId, userId));
    }
}