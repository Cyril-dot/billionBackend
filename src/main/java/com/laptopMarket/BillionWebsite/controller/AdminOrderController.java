package com.laptopMarket.BillionWebsite.controller;

import com.laptopMarket.BillionWebsite.Config.Security.AdminPrincipal;
import com.laptopMarket.BillionWebsite.Config.Security.UserPrincipal;
import com.laptopMarket.BillionWebsite.entity.OrderStatus;
import com.laptopMarket.BillionWebsite.dto.OrderResponse;
import com.laptopMarket.BillionWebsite.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SHOP_OWNER')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * PATCH /api/v1/admin/orders/{orderId}/status?status=SHIPPED
     * Update order status.
     * Flow: PENDING → CONFIRMED → SHIPPED → DELIVERED
     * Blocked if order is CANCELLED or already DELIVERED.
     * Valid values: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        log.info("➡️  PATCH /admin/orders/{}/status → {}", orderId, status);
        return ResponseEntity.ok(adminOrderService.updateOrderStatus(orderId, status));
    }

    /**
     * PATCH /api/v1/admin/orders/{orderId}/cancel
     * Cancel an order. Cannot cancel a DELIVERED order.
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId) {
        log.info("➡️  PATCH /admin/orders/{}/cancel", orderId);
        return ResponseEntity.ok(adminOrderService.cancelOrder(orderId));
    }

    // ════════════════════════════════════════════════════════
    // VIEW ALL ORDERS
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/admin/orders
     * All orders ever placed, newest first
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("➡️  GET /admin/orders");
        return ResponseEntity.ok(adminOrderService.getAllOrders());
    }

    /**
     * GET /api/v1/admin/orders/{orderId}
     * Get full details of a single order
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId) {
        log.info("➡️  GET /admin/orders/{}", orderId);
        return ResponseEntity.ok(adminOrderService.getOrderById(orderId));
    }

    /**
     * GET /api/v1/admin/orders/recent?limit=10
     * Last N orders placed (most recent first)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<OrderResponse>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("➡️  GET /admin/orders/recent?limit={}", limit);
        return ResponseEntity.ok(adminOrderService.getRecentOrders(limit));
    }

    /**
     * GET /api/v1/admin/orders/highest-value?limit=10
     * Top N orders by total amount (highest value first)
     */
    @GetMapping("/highest-value")
    public ResponseEntity<List<OrderResponse>> getHighestValueOrders(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("➡️  GET /admin/orders/highest-value?limit={}", limit);
        return ResponseEntity.ok(adminOrderService.getHighestValueOrders(limit));
    }

    // ════════════════════════════════════════════════════════
    // FILTER BY STATUS
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/admin/orders/status/{status}
     * Filter orders by status.
     * Valid values: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        log.info("➡️  GET /admin/orders/status/{}", status);
        return ResponseEntity.ok(adminOrderService.getOrdersByStatus(status));
    }

    /** GET /api/v1/admin/orders/status/pending */
    @GetMapping("/status/pending")
    public ResponseEntity<List<OrderResponse>> getPendingOrders() {
        log.info("➡️  GET /admin/orders/status/pending");
        return ResponseEntity.ok(adminOrderService.getPendingOrders());
    }

    /** GET /api/v1/admin/orders/status/confirmed */
    @GetMapping("/status/confirmed")
    public ResponseEntity<List<OrderResponse>> getConfirmedOrders() {
        log.info("➡️  GET /admin/orders/status/confirmed");
        return ResponseEntity.ok(adminOrderService.getConfirmedOrders());
    }

    /** GET /api/v1/admin/orders/status/shipped */
    @GetMapping("/status/shipped")
    public ResponseEntity<List<OrderResponse>> getShippedOrders() {
        log.info("➡️  GET /admin/orders/status/shipped");
        return ResponseEntity.ok(adminOrderService.getShippedOrders());
    }

    /** GET /api/v1/admin/orders/status/delivered */
    @GetMapping("/status/delivered")
    public ResponseEntity<List<OrderResponse>> getDeliveredOrders() {
        log.info("➡️  GET /admin/orders/status/delivered");
        return ResponseEntity.ok(adminOrderService.getDeliveredOrders());
    }

    /** GET /api/v1/admin/orders/status/cancelled */
    @GetMapping("/status/cancelled")
    public ResponseEntity<List<OrderResponse>> getCancelledOrders() {
        log.info("➡️  GET /admin/orders/status/cancelled");
        return ResponseEntity.ok(adminOrderService.getCancelledOrders());
    }

    // ════════════════════════════════════════════════════════
    // FILTER BY DATE
    // ════════════════════════════════════════════════════════

    /** GET /api/v1/admin/orders/date/today */
    @GetMapping("/date/today")
    public ResponseEntity<List<OrderResponse>> getOrdersToday() {
        log.info("➡️  GET /admin/orders/date/today");
        return ResponseEntity.ok(adminOrderService.getOrdersToday());
    }

    /** GET /api/v1/admin/orders/date/week */
    @GetMapping("/date/week")
    public ResponseEntity<List<OrderResponse>> getOrdersThisWeek() {
        log.info("➡️  GET /admin/orders/date/week");
        return ResponseEntity.ok(adminOrderService.getOrdersThisWeek());
    }

    /** GET /api/v1/admin/orders/date/month */
    @GetMapping("/date/month")
    public ResponseEntity<List<OrderResponse>> getOrdersThisMonth() {
        log.info("➡️  GET /admin/orders/date/month");
        return ResponseEntity.ok(adminOrderService.getOrdersThisMonth());
    }

    /**
     * GET /api/v1/admin/orders/date/range?from=2025-01-01T00:00:00&to=2025-01-31T23:59:59
     * Get orders within a custom date and time range
     */
    @GetMapping("/date/range")
    public ResponseEntity<List<OrderResponse>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.info("➡️  GET /admin/orders/date/range?from={}&to={}", from, to);
        return ResponseEntity.ok(adminOrderService.getOrdersByDateRange(from, to));
    }

    // ════════════════════════════════════════════════════════
    // FILTER BY USER
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/admin/orders/user/{userId}
     * Get all orders placed by a specific customer
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(
            @PathVariable UUID userId) {
        log.info("➡️  GET /admin/orders/user/{}", userId);
        return ResponseEntity.ok(adminOrderService.getOrdersByUser(userId));
    }

    // ════════════════════════════════════════════════════════
    // DASHBOARD / ANALYTICS
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/admin/orders/summary
     * Dashboard stats: total orders, count per status, total revenue, weekly revenue
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getOrderSummary() {
        log.info("➡️  GET /admin/orders/summary");
        return ResponseEntity.ok(adminOrderService.getOrderSummary());
    }

    /**
     * GET /api/v1/admin/orders/analytics/daily
     * Orders count grouped by day for the last 7 days.
     * Useful for building a sales chart.
     */
    @GetMapping("/analytics/daily")
    public ResponseEntity<Map<String, Long>> getDailyOrderCount() {
        log.info("➡️  GET /admin/orders/analytics/daily");
        return ResponseEntity.ok(adminOrderService.getOrderCountPerDayLastWeek());
    }
}