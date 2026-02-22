package com.laptopMarket.BillionWebsite.controller;

import com.laptopMarket.BillionWebsite.Config.Security.AdminPrincipal;
import com.laptopMarket.BillionWebsite.Config.Security.TokenService;
import com.laptopMarket.BillionWebsite.Config.Security.UserPrincipal;
import com.laptopMarket.BillionWebsite.dto.*;
import com.laptopMarket.BillionWebsite.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService  userService;

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

    private AdminPrincipal adminPrincipal(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AdminPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        AdminPrincipal adminPrincipal = (AdminPrincipal) principal;
        log.debug("Successfully retrieved AdminPrincipal for user: {} (ID: {})",
                adminPrincipal.getUsername(), adminPrincipal.getOwnerId());

        return adminPrincipal;
    }

    /**
     * GET /api/v1/user/profile
     * Returns the logged-in user's own profile
     */
    @GetMapping("/api/v1/user/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> getMyProfile() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  GET /user/profile - userId: {}", userId);
        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    /**
     * PUT /api/v1/user/profile
     * Update name or phone — user cannot change email or role
     * Body: { "firstName": "", "lastName": "", "phone": "" }
     */
    @PutMapping("/api/v1/user/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UpdateUserRequest request) {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("➡️  PUT /user/profile - userId: {}", userId);
        return ResponseEntity.ok(userService.updateMyProfile(userId, request));
    }

    // ════════════════════════════════════════════════════════
    // ADMIN — /api/v1/admin/users/**
    // Requires: ADMIN token
    // ════════════════════════════════════════════════════════


    // to get admin profile
    @GetMapping("/api/v1/admin/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminResponse> adminProfile(){
        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();
        log.info(" get admin profile detais-  admin id: {}", adminId);
        return ResponseEntity.ok(userService.adminProfile(adminId));
    }


    /**
     * GET /api/v1/admin/users
     * Returns total registered user count + full list with emails
     */
    @GetMapping("/api/v1/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummaryResponse> getAllUsers() {
        log.info("➡️  GET /admin/users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /api/v1/admin/users/{userId}
     * Get full details of a single user by their UUID
     */
    @GetMapping("/api/v1/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID userId) {
        log.info("➡️  GET /admin/users/{}", userId);
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * GET /api/v1/admin/users/search/email?q=john
     * Search users whose email contains the query string
     */
    @GetMapping("/api/v1/admin/users/search/email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> searchByEmail(
            @RequestParam String q) {
        log.info("➡️  GET /admin/users/search/email?q={}", q);
        return ResponseEntity.ok(userService.searchUsersByEmail(q));
    }

    /**
     * GET /api/v1/admin/users/search/name?q=john
     * Search users by first name or last name
     */
    @GetMapping("/api/v1/admin/users/search/name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> searchByName(
            @RequestParam String q) {
        log.info("➡️  GET /admin/users/search/name?q={}", q);
        return ResponseEntity.ok(userService.searchUsersByName(q));
    }

    /**
     * DELETE /api/v1/admin/users/{userId}
     * Permanently remove a user and all their data
     */
    @DeleteMapping("/api/v1/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> removeUser(
            @PathVariable UUID userId) {
        log.info("➡️  DELETE /admin/users/{}", userId);
        return ResponseEntity.ok(userService.removeUser(userId));
    }
}