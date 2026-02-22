package com.laptopMarket.BillionWebsite.controller;

import com.laptopMarket.BillionWebsite.Config.Security.AdminPrincipal;
import com.laptopMarket.BillionWebsite.dto.ProductRequest;
import com.laptopMarket.BillionWebsite.dto.ProductResponse;
import com.laptopMarket.BillionWebsite.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SHOP_OWNER')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    private AdminPrincipal adminPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("User not authenticated");
        Object principal = auth.getPrincipal();
        if (!(principal instanceof AdminPrincipal)) throw new RuntimeException("Invalid authentication principal");
        return (AdminPrincipal) principal;
    }

    /**
     * POST /api/v1/admin/products
     * Add a new product with 1–10 images.
     * Field name for images: "images" (multipart list)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> addProduct(
            @RequestParam("name")                                       String name,
            @RequestParam("price")                                      String price,
            @RequestParam("category")                                   String category,
            @RequestParam(value = "description",  required = false)     String description,
            @RequestParam(value = "brand",        required = false)     String brand,
            @RequestParam(value = "stock",        defaultValue = "0")   Integer stock,
            @RequestParam(value = "images",       required = false)     List<MultipartFile> images
    ) throws IOException {
        UUID adminId = adminPrincipal().getOwnerId();
        log.info("➡️  POST /admin/products - name: {} | images: {} | admin: {}",
                name, images != null ? images.size() : 0, adminId);

        ProductRequest request = buildRequest(name, price, category, description, brand, stock);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminProductService.addProduct(request, images, adminId));
    }

    /**
     * PUT /api/v1/admin/products/{productId}
     * Update product details + optionally add new images and/or remove existing ones.
     *
     * @param newImages      New image files to append (optional)
     * @param imageIdsToDelete Comma-separated IDs of ProductImage rows to delete (optional)
     */
    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @RequestParam("name")                                       String name,
            @RequestParam("price")                                      String price,
            @RequestParam("category")                                   String category,
            @RequestParam(value = "description",  required = false)     String description,
            @RequestParam(value = "brand",        required = false)     String brand,
            @RequestParam(value = "stock",        required = false)     Integer stock,
            @RequestParam(value = "images",       required = false)     List<MultipartFile> newImages,
            @RequestParam(value = "deleteImageIds", required = false)   List<Long> imageIdsToDelete
    ) throws IOException {
        UUID adminId = adminPrincipal().getOwnerId();
        log.info("➡️  PUT /admin/products/{} - newImages: {} | toDelete: {} | admin: {}",
                productId,
                newImages != null ? newImages.size() : 0,
                imageIdsToDelete,
                adminId);

        ProductRequest request = buildRequest(name, price, category, description, brand, stock);
        return ResponseEntity.ok(
                adminProductService.updateProduct(productId, request, newImages, imageIdsToDelete, adminId));
    }

    /**
     * PUT /api/v1/admin/products/{productId}/images/replace
     * Nuke all existing images and upload fresh ones.
     */
    @PutMapping(value = "/{productId}/images/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> replaceAllImages(
            @PathVariable Long productId,
            @RequestParam("images") List<MultipartFile> images
    ) throws IOException {
        log.info("➡️  PUT /admin/products/{}/images/replace - {} image(s)", productId, images.size());
        return ResponseEntity.ok(adminProductService.replaceAllImages(productId, images));
    }

    /**
     * PATCH /api/v1/admin/products/{productId}/stock?quantity=50
     */
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long productId,
            @RequestParam int quantity) {
        log.info("➡️  PATCH /admin/products/{}/stock → {}", productId, quantity);
        return ResponseEntity.ok(adminProductService.updateStock(productId, quantity));
    }

    /**
     * DELETE /api/v1/admin/products/{productId}
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<String> removeProduct(@PathVariable Long productId) throws IOException {
        log.info("➡️  DELETE /admin/products/{}", productId);
        return ResponseEntity.ok(adminProductService.removeProduct(productId));
    }

    // ── READ ENDPOINTS (unchanged) ───────────────────────────

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(adminProductService.getAllProducts());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ProductResponse>> getMyProducts() {
        return ResponseEntity.ok(adminProductService.getMyProducts(adminPrincipal().getOwnerId()));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(adminProductService.getProductById(productId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(adminProductService.searchByName(name));
    }

    @GetMapping("/search/keyword")
    public ResponseEntity<List<ProductResponse>> searchByKeyword(@RequestParam String q) {
        return ResponseEntity.ok(adminProductService.searchByKeyword(q));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(adminProductService.getByCategory(category));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(adminProductService.getAllCategories());
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        return ResponseEntity.ok(adminProductService.getAllBrands());
    }

    // ── PRIVATE ──────────────────────────────────────────────
    private ProductRequest buildRequest(String name, String price, String category,
                                         String description, String brand, Integer stock) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setPrice(new java.math.BigDecimal(price));
        req.setCategory(category);
        req.setDescription(description);
        req.setBrand(brand);
        req.setStock(stock != null ? stock : 0);
        return req;
    }
}