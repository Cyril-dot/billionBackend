package com.laptopMarket.BillionWebsite.service;

import com.laptopMarket.BillionWebsite.dto.ProductRequest;
import com.laptopMarket.BillionWebsite.dto.ProductResponse;
import com.laptopMarket.BillionWebsite.entity.Product;
import com.laptopMarket.BillionWebsite.entity.ProductImage;
import com.laptopMarket.BillionWebsite.entity.ShopOwner;
import com.laptopMarket.BillionWebsite.entity.User;
import com.laptopMarket.BillionWebsite.entity.repo.AdminRepo;
import com.laptopMarket.BillionWebsite.entity.repo.ProductRepository;
import com.laptopMarket.BillionWebsite.entity.repo.ProductImageRepository;
import com.laptopMarket.BillionWebsite.entity.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository     productRepository;
    private final ProductImageRepository productImageRepository;
    private final AdminRepo             shopOwnerRepository;
    private final UserRepo              userRepository;
    private final CloudinaryService     cloudinaryService;
    private final EmailService          emailService;

    // ── ADD PRODUCT ──────────────────────────────────────────
    public ProductResponse addProduct(ProductRequest request,
                                      List<MultipartFile> images,
                                      UUID adminId) throws IOException {
        ShopOwner admin = shopOwnerRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));

        Product product = Product.builder()
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .category(request.getCategory().toLowerCase())
            .brand(request.getBrand())
            .stock(request.getStock())
            .addedBy(admin)
            .build();

        Product saved = productRepository.save(product); // save first to get ID

        // Upload all images
        if (images != null && !images.isEmpty()) {
            List<ProductImage> productImages = uploadImages(images, saved);
            productImageRepository.saveAll(productImages);
            saved.setImages(productImages);
        }

        System.out.println("✅ Product added: [" + saved.getName() + "] with "
            + saved.getImages().size() + " image(s)");

        // Email all users
        List<User> allUsers = userRepository.findAll();
        if (!allUsers.isEmpty()) {
            emailService.announceNewProductToAllUsers(allUsers, saved, admin.getShopName());
        }

        return mapToResponse(saved);
    }

    // ── UPDATE PRODUCT ───────────────────────────────────────
    public ProductResponse updateProduct(Long productId, ProductRequest request,
                                         List<MultipartFile> newImages,
                                         List<Long> imageIdsToDelete,
                                         UUID adminId) throws IOException {
        Product product = findProductById(productId);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory().toLowerCase());
        product.setBrand(request.getBrand());
        product.setStock(request.getStock());

        // Delete specific images if requested
        if (imageIdsToDelete != null && !imageIdsToDelete.isEmpty()) {
            for (Long imageId : imageIdsToDelete) {
                productImageRepository.findById(imageId).ifPresent(img -> {
                    try {
                        cloudinaryService.deleteImage(img.getImagePublicId());
                        productImageRepository.delete(img);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete image: " + imageId, e);
                    }
                });
            }
            // Refresh the list after deletion
            product.getImages().removeIf(img -> imageIdsToDelete.contains(img.getId()));
        }

        // Upload and append new images
        if (newImages != null && !newImages.isEmpty()) {
            // Current max order
            int currentMaxOrder = product.getImages().stream()
                .mapToInt(ProductImage::getDisplayOrder)
                .max().orElse(-1);

            List<ProductImage> addedImages = uploadImages(newImages, product, currentMaxOrder + 1);
            productImageRepository.saveAll(addedImages);
            product.getImages().addAll(addedImages);
        }

        Product updated = productRepository.save(product);
        System.out.println("✅ Product updated: [" + updated.getName() + "] — "
            + updated.getImages().size() + " image(s)");
        return mapToResponse(updated);
    }

    // ── REPLACE ALL IMAGES ───────────────────────────────────
    public ProductResponse replaceAllImages(Long productId,
                                             List<MultipartFile> newImages) throws IOException {
        Product product = findProductById(productId);

        // Delete all existing images from Cloudinary + DB
        for (ProductImage img : product.getImages()) {
            cloudinaryService.deleteImage(img.getImagePublicId());
        }
        productImageRepository.deleteAll(product.getImages());
        product.getImages().clear();

        // Upload new images
        if (newImages != null && !newImages.isEmpty()) {
            List<ProductImage> uploaded = uploadImages(newImages, product);
            productImageRepository.saveAll(uploaded);
            product.setImages(uploaded);
        }

        return mapToResponse(productRepository.save(product));
    }

    // ── UPDATE STOCK ONLY ────────────────────────────────────
    public ProductResponse updateStock(Long productId, int newStock) {
        Product product = findProductById(productId);
        product.setStock(newStock);
        productRepository.save(product);
        return mapToResponse(product);
    }

    // ── REMOVE PRODUCT ───────────────────────────────────────
    public String removeProduct(Long productId) throws IOException {
        Product product = findProductById(productId);

        // Delete all images from Cloudinary
        for (ProductImage img : product.getImages()) {
            cloudinaryService.deleteImage(img.getImagePublicId());
        }

        productRepository.delete(product); // cascade deletes ProductImage rows
        System.out.println("🗑️  Product removed: [" + product.getName() + "]");
        return "Product \"" + product.getName() + "\" removed successfully.";
    }

    // ── READ OPERATIONS ──────────────────────────────────────
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getMyProducts(UUID adminId) {
        return productRepository.findByAddedById(adminId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long productId) {
        return mapToResponse(findProductById(productId));
    }

    public List<ProductResponse> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> searchByKeyword(String keyword) {
        return productRepository.searchByKeyword(keyword).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<String> getAllCategories() { return productRepository.findAllCategories(); }
    public List<String> getAllBrands()     { return productRepository.findAllBrands(); }

    // ── PRIVATE HELPERS ──────────────────────────────────────

    /** Upload a list of files starting at displayOrder = 0 */
    private List<ProductImage> uploadImages(List<MultipartFile> files, Product product) throws IOException {
        return uploadImages(files, product, 0);
    }

    /** Upload a list of files starting at a given displayOrder offset */
    private List<ProductImage> uploadImages(List<MultipartFile> files,
                                             Product product,
                                             int startOrder) throws IOException {
        List<ProductImage> result = new ArrayList<>();
        int order = startOrder;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            Map uploadResult = cloudinaryService.uploadImage(file, "laptopstore/products");

            ProductImage image = ProductImage.builder()
                .imageUrl((String) uploadResult.get("secure_url"))
                .imagePublicId((String) uploadResult.get("public_id"))
                .displayOrder(order++)
                .product(product)
                .build();

            result.add(image);
        }
        return result;
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    ProductResponse mapToResponse(Product p) {
        // Build list of optimized URLs in display order
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
            .imageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))   // primary image
            .imageUrls(imageUrls)                                        // all images
            .addedByAdmin(p.getAddedBy() != null ? p.getAddedBy().getName() : "N/A")
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }
}