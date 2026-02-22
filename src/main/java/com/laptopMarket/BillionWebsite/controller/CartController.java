package com.laptopMarket.BillionWebsite.controller;

import com.laptopMarket.BillionWebsite.Config.Security.TokenService;
import com.laptopMarket.BillionWebsite.Config.Security.UserPrincipal;
import com.laptopMarket.BillionWebsite.dto.AddToCartRequest;
import com.laptopMarket.BillionWebsite.dto.CartResponse;
import com.laptopMarket.BillionWebsite.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class CartController {

    private final CartService  cartService;


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
     * GET /api/v1/cart
     * View the logged-in user's cart with item list and grand total
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  GET /cart - userId: {}", userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /**
     * GET /api/v1/cart/count
     * Returns only the number of items in the cart — useful for the cart badge icon
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getCartCount() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  GET /cart/count - userId: {}", userId);
        return ResponseEntity.ok(cartService.getCartItemCount(userId));
    }

    /**
     * POST /api/v1/cart
     * Add a product to the cart.
     * If the product is already in the cart, quantity is increased.
     * Body: { "productId": 1, "quantity": 2 }
     */
    @PostMapping
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  POST /cart - userId: {} | productId: {} | qty: {}",
                userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addToCart(userId, request));
    }

    /**
     * PATCH /api/v1/cart/{cartItemId}?quantity=3
     * Update the quantity of a specific cart item.
     * If quantity = 0, the item is removed from the cart.
     */
    @PatchMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> updateQuantity(@PathVariable Long cartItemId,
            @RequestParam int quantity) {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  PATCH /cart/{} → qty: {} - userId: {}", cartItemId, quantity, userId);
        return ResponseEntity.ok(cartService.updateQuantity(userId, cartItemId, quantity));
    }

    /**
     * DELETE /api/v1/cart/{cartItemId}
     * Remove a single item from the cart
     */
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> removeFromCart(@PathVariable Long cartItemId) {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  DELETE /cart/{} - userId: {}", cartItemId, userId);
        return ResponseEntity.ok(cartService.removeFromCart(userId, cartItemId));
    }

    /**
     * DELETE /api/v1/cart
     * Clear the entire cart
     */
    @DeleteMapping
    public ResponseEntity<String> clearCart() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  DELETE /cart (clear all) - userId: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared successfully.");
    }

}