package com.laptopMarket.BillionWebsite.controller;

import com.laptopMarket.BillionWebsite.dto.*;
import com.laptopMarket.BillionWebsite.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;

    // ════════════════════════════════════════════════════════
    // CUSTOMER AUTH
    // ════════════════════════════════════════════════════════

    /** POST /api/v1/auth/user/register */
    @PostMapping("/user/register")
    public ResponseEntity<AuthResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        log.info("➡️  POST /user/register - {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.registerUser(request));
    }

    /** POST /api/v1/auth/user/login */
    @PostMapping("/user/login")
    public ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody LoginRequest request) {
        log.info("➡️  POST /user/login - {}", request.getEmail());
        return ResponseEntity.ok(registrationService.loginUser(request));
    }

    // ════════════════════════════════════════════════════════
    // ADMIN / SHOP OWNER AUTH
    // ════════════════════════════════════════════════════════

    /** POST /api/v1/auth/admin/register */
    @PostMapping("/admin/register")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody AdminRegistrationRequest request) {
        log.info("➡️  POST /admin/register - {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.registerAdmin(request));
    }

    /** POST /api/v1/auth/admin/login */
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(
            @Valid @RequestBody LoginRequest request) {
        log.info("➡️  POST /admin/login - {}", request.getEmail());
        return ResponseEntity.ok(registrationService.loginAdmin(request));
    }

}