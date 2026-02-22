package com.laptopMarket.BillionWebsite.service;

import com.laptopMarket.BillionWebsite.dto.OrderItemResponse;
import com.laptopMarket.BillionWebsite.dto.OrderResponse;
import com.laptopMarket.BillionWebsite.entity.Order;
import com.laptopMarket.BillionWebsite.entity.OrderStatus;
import com.laptopMarket.BillionWebsite.entity.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AdminOrderService — ADMIN ONLY
 * ─────────────────────────────────────────────────
 * Only admins can:
 *   - Update order status (PENDING → CONFIRMED → SHIPPED → DELIVERED)
 *   - Cancel any order
 *   - View all orders across all users
 *   - Filter orders by status, date, user, value
 * ─────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;

    // ═══════════════════════════════════════════════════════════
    // STATUS MANAGEMENT — ADMIN ONLY
    // ═══════════════════════════════════════════════════════════

    /**
     * Update the status of any order.
     * ONLY admins should call this — do NOT expose this in a user-facing controller.
     * Flow: PENDING → CONFIRMED → SHIPPED → DELIVERED
     */
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = findOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        // Block updating a cancelled or already delivered order
        if (oldStatus == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot update a cancelled order.");
        }
        if (oldStatus == OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
            throw new RuntimeException("Order already delivered. Only cancellation is allowed.");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        System.out.println("📦 [ADMIN] Order #" + orderId + " status: " + oldStatus + " → " + newStatus);
        return mapToResponse(order);
    }

    /**
     * Cancel any order — ADMIN ONLY.
     * Customers cannot cancel orders themselves.
     */
    public OrderResponse cancelOrder(Long orderId) {
        Order order = findOrderById(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel an already delivered order.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        System.out.println("❌ [ADMIN] Order #" + orderId + " has been cancelled.");
        return mapToResponse(order);
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW ORDERS BY STATUS — ADMIN ONLY
    // ═══════════════════════════════════════════════════════════

    // All PENDING orders (need to be confirmed)
    public List<OrderResponse> getPendingOrders() {
        return getOrdersByStatus(OrderStatus.PENDING);
    }

    // All CONFIRMED orders (packed, ready to ship)
    public List<OrderResponse> getConfirmedOrders() {
        return getOrdersByStatus(OrderStatus.CONFIRMED);
    }

    // All SHIPPED orders (on the way)
    public List<OrderResponse> getShippedOrders() {
        return getOrdersByStatus(OrderStatus.SHIPPED);
    }

    // All DELIVERED orders (completed)
    public List<OrderResponse> getDeliveredOrders() {
        return getOrdersByStatus(OrderStatus.DELIVERED);
    }

    // All CANCELLED orders
    public List<OrderResponse> getCancelledOrders() {
        return getOrdersByStatus(OrderStatus.CANCELLED);
    }

    // Generic — filter by any status (used by the above methods + controller)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        System.out.println("📋 [ADMIN] Orders with status [" + status + "]: " + orders.size());
        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW ALL ORDERS — ADMIN ONLY
    // ═══════════════════════════════════════════════════════════

    // All orders ever placed, newest first
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        System.out.println("📋 [ADMIN] Total orders: " + orders.size());
        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // Single order full details
    public OrderResponse getOrderById(Long orderId) {
        return mapToResponse(findOrderById(orderId));
    }

    // Last N orders placed (most recent)
    public List<OrderResponse> getRecentOrders(int limit) {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().limit(limit)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Top N highest value orders
    public List<OrderResponse> getHighestValueOrders(int limit) {
        return orderRepository.findAllByOrderByTotalAmountDesc()
                .stream().limit(limit)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW ORDERS BY DATE — ADMIN ONLY
    // ═══════════════════════════════════════════════════════════

    // Orders placed today
    public List<OrderResponse> getOrdersToday() {
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime end   = start.plusDays(1);
        return getOrdersByDateRange(start, end);
    }

    // Orders placed in the last 7 days
    public List<OrderResponse> getOrdersThisWeek() {
        LocalDateTime start = LocalDateTime.now().minusDays(7).truncatedTo(ChronoUnit.DAYS);
        return getOrdersByDateRange(start, LocalDateTime.now());
    }

    // Orders placed this calendar month
    public List<OrderResponse> getOrdersThisMonth() {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        return getOrdersByDateRange(start, LocalDateTime.now());
    }

    // Orders in any custom date range
    public List<OrderResponse> getOrdersByDateRange(LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        System.out.println("📅 [ADMIN] Orders from " + from.toLocalDate() + " to " + to.toLocalDate() + ": " + orders.size());
        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW ORDERS BY USER — ADMIN ONLY
    // ═══════════════════════════════════════════════════════════

    // All orders placed by a specific customer
    public List<OrderResponse> getOrdersByUser(UUID userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        System.out.println("👤 [ADMIN] Orders for user [" + userId + "]: " + orders.size());
        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // DASHBOARD SUMMARY — ADMIN ONLY
    // ═══════════════════════════════════════════════════════════

    // Returns order counts by status + total and weekly revenue
    public Map<String, Object> getOrderSummary() {
        List<Order> all = orderRepository.findAll();

        long total     = all.size();
        long pending   = all.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long confirmed = all.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long shipped   = all.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).count();
        long delivered = all.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long cancelled = all.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        BigDecimal totalRevenue = all.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weekRevenue = orderRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        LocalDateTime.now().minusDays(7), LocalDateTime.now())
                .stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalOrders",     total);
        summary.put("pending",         pending);
        summary.put("confirmed",       confirmed);
        summary.put("shipped",         shipped);
        summary.put("delivered",       delivered);
        summary.put("cancelled",       cancelled);
        summary.put("totalRevenue",    "$" + totalRevenue);
        summary.put("revenueThisWeek", "$" + weekRevenue);

        System.out.println("═══════════════════════════════════════");
        System.out.println("📊 [ADMIN] ORDER SUMMARY");
        summary.forEach((k, v) -> System.out.println("   " + k + ": " + v));
        System.out.println("═══════════════════════════════════════");

        return summary;
    }

    // Orders count grouped per day for the last 7 days (for sales chart)
    public Map<String, Long> getOrderCountPerDayLastWeek() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Order> orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(weekAgo, LocalDateTime.now());

        Map<String, Long> perDay = orders.stream().collect(
                Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()
                )
        );

        System.out.println("📈 [ADMIN] Orders per day (last 7 days):");
        perDay.forEach((day, count) -> System.out.println("   " + day + ": " + count + " order(s)"));
        return perDay;
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream().map(item ->
                OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .imageUrl(item.getProduct().getPrimaryImageUrl())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build()
        ).collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .customerEmail(order.getUser().getEmail())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .deliveryAddress(order.getDeliveryAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }
}