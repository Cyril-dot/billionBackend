package com.laptopMarket.BillionWebsite.controller;

import com.laptopMarket.BillionWebsite.dto.ProductResponse;
import com.laptopMarket.BillionWebsite.service.UserProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserProductController {

    private final UserProductService userProductService;

    // ════════════════════════════════════════════════════════
    // BROWSE — open to all (no auth required)
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/products
     * Get all products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("➡️  GET /products");
        return ResponseEntity.ok(userProductService.getAllProducts());
    }

    /**
     * GET /api/v1/products/{productId}
     * Get a single product by ID
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long productId) {
        log.info("➡️  GET /products/{}", productId);
        return ResponseEntity.ok(userProductService.getProductById(productId));
    }

    /**
     * GET /api/v1/products/in-stock
     * Get only products that have stock > 0
     */
    @GetMapping("/in-stock")
    public ResponseEntity<List<ProductResponse>> getInStockProducts() {
        log.info("➡️  GET /products/in-stock");
        return ResponseEntity.ok(userProductService.getInStockProducts());
    }

    // ════════════════════════════════════════════════════════
    // CATEGORIZE & FILTER
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/products/categories
     * Returns all distinct categories e.g. ["laptop","mouse","keyboard"]
     * Use this to build filter/browse dropdowns in the frontend
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(userProductService.getAllCategories());
    }

    /**
     * GET /api/v1/products/brands
     * Returns all distinct brands e.g. ["Dell","HP","Apple"]
     */
    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        return ResponseEntity.ok(userProductService.getAllBrands());
    }

    /**
     * GET /api/v1/products/category/{category}
     * Browse all products in a category e.g. /category/laptop
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getByCategory(
            @PathVariable String category) {
        log.info("➡️  GET /products/category/{}", category);
        return ResponseEntity.ok(userProductService.getByCategory(category));
    }

    /**
     * GET /api/v1/products/brand/{brand}
     * Browse all products by a brand e.g. /brand/dell
     */
    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<ProductResponse>> getByBrand(
            @PathVariable String brand) {
        log.info("➡️  GET /products/brand/{}", brand);
        return ResponseEntity.ok(userProductService.getByBrand(brand));
    }

    /**
     * GET /api/v1/products/price-range?min=100&max=500
     * Filter products within a price range
     */
    @GetMapping("/price-range")
    public ResponseEntity<List<ProductResponse>> getByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        log.info("➡️  GET /products/price-range?min={}&max={}", min, max);
        return ResponseEntity.ok(userProductService.getByPriceRange(min, max));
    }

    /**
     * GET /api/v1/products/filter?category=laptop&min=500&max=2000
     * Filter by category AND price range together
     */
    @GetMapping("/filter")
    public ResponseEntity<List<ProductResponse>> filterByCategoryAndPrice(
            @RequestParam String category,
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        log.info("➡️  GET /products/filter?category={}&min={}&max={}", category, min, max);
        return ResponseEntity.ok(userProductService.getByCategoryAndPriceRange(category, min, max));
    }

    // ════════════════════════════════════════════════════════
    // SEARCH
    // ════════════════════════════════════════════════════════

    /**
     * GET /api/v1/products/search?name=dell
     * Search products by name (partial, case-insensitive)
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchByName(
            @RequestParam String name) {
        log.info("➡️  GET /products/search?name={}", name);
        return ResponseEntity.ok(userProductService.searchByName(name));
    }

    /**
     * GET /api/v1/products/search/keyword?q=gaming laptop
     * Full keyword search across name + description + brand
     */
    @GetMapping("/search/keyword")
    public ResponseEntity<List<ProductResponse>> searchByKeyword(
            @RequestParam String q) {
        log.info("➡️  GET /products/search/keyword?q={}", q);
        return ResponseEntity.ok(userProductService.searchByKeyword(q));
    }

    /**
     * GET /api/v1/products/search/filter?name=dell&category=laptop
     * Search by name AND filter by category
     */
    @GetMapping("/search/filter")
    public ResponseEntity<List<ProductResponse>> searchByNameAndCategory(
            @RequestParam String name,
            @RequestParam String category) {
        log.info("➡️  GET /products/search/filter?name={}&category={}", name, category);
        return ResponseEntity.ok(userProductService.searchByNameAndCategory(name, category));
    }

    /**
     * GET /api/v1/products/search/brand?name=xps&brand=dell
     * Search by name AND filter by brand
     */
    @GetMapping("/search/brand")
    public ResponseEntity<List<ProductResponse>> searchByNameAndBrand(
            @RequestParam String name,
            @RequestParam String brand) {
        log.info("➡️  GET /products/search/brand?name={}&brand={}", name, brand);
        return ResponseEntity.ok(userProductService.searchByNameAndBrand(name, brand));
    }
}