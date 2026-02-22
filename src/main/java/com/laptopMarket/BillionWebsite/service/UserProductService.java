package com.laptopMarket.BillionWebsite.service;

import com.laptopMarket.BillionWebsite.dto.ProductResponse;
import com.laptopMarket.BillionWebsite.entity.Product;
import com.laptopMarket.BillionWebsite.entity.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserProductService — CUSTOMER FACING
 * Handles: browse, search, filter, categorize — READ ONLY
 */
@Service
@RequiredArgsConstructor
public class UserProductService {

    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    // ── GET ALL PRODUCTS ─────────────────────────────────────
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── GET SINGLE PRODUCT ───────────────────────────────────
    public ProductResponse getProductById(Long id) {
        return mapToResponse(findProductById(id));
    }

    // ── IN-STOCK PRODUCTS ONLY ───────────────────────────────
    public List<ProductResponse> getInStockProducts() {
        return productRepository.findByStockGreaterThan(0)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY CATEGORY ───────────────────────────────────
    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY BRAND ──────────────────────────────────────
    public List<ProductResponse> getByBrand(String brand) {
        return productRepository.findByBrandIgnoreCase(brand)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── SEARCH BY NAME ───────────────────────────────────────
    public List<ProductResponse> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── FULL KEYWORD SEARCH ───────────────────────────────────
    public List<ProductResponse> searchByKeyword(String keyword) {
        return productRepository.searchByKeyword(keyword)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── SEARCH BY NAME + FILTER BY CATEGORY ──────────────────
    public List<ProductResponse> searchByNameAndCategory(String name, String category) {
        return productRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCase(name, category)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── SEARCH BY NAME + FILTER BY BRAND ─────────────────────
    public List<ProductResponse> searchByNameAndBrand(String name, String brand) {
        return productRepository.findByNameContainingIgnoreCaseAndBrandIgnoreCase(name, brand)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── FILTER BY PRICE RANGE ────────────────────────────────
    public List<ProductResponse> getByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceBetween(min, max)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── FILTER BY CATEGORY + PRICE RANGE ─────────────────────
    public List<ProductResponse> getByCategoryAndPriceRange(String category, BigDecimal min, BigDecimal max) {
        return productRepository.findByCategoryIgnoreCaseAndPriceBetween(category, min, max)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── ALL CATEGORIES ────────────────────────────────────────
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    // ── ALL BRANDS ────────────────────────────────────────────
    public List<String> getAllBrands() {
        return productRepository.findAllBrands();
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────
    private Product findProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    private ProductResponse mapToResponse(Product p) {
        // Build optimized URLs for ALL images in display order
        List<String> imageUrls = p.getImages().stream()
            .map(img -> cloudinaryService.getOptimizedImageUrl(img.getImagePublicId()))
            .collect(Collectors.toList());

        return ProductResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .description(p.getDescription())
            .price(p.getPrice())
            .category(p.getCategory())
            .brand(p.getBrand())
            .stock(p.getStock())
            .imageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))  // primary image
            .imageUrls(imageUrls)                                       // all images
            .addedByAdmin(p.getAddedBy() != null ? p.getAddedBy().getName() : "N/A")
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }
}