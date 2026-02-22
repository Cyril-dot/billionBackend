package com.laptopMarket.BillionWebsite.entity.repo;

import com.laptopMarket.BillionWebsite.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ── CATEGORY ─────────────────────────────────────────────
    List<Product> findByCategory(String category);
    List<Product> findByCategoryIgnoreCase(String category);

    // ── BRAND ────────────────────────────────────────────────
    List<Product> findByBrand(String brand);
    List<Product> findByBrandIgnoreCase(String brand);

    // ── NAME SEARCH ──────────────────────────────────────────
    List<Product> findByNameContainingIgnoreCase(String name);

    // ── NAME + CATEGORY ──────────────────────────────────────
    List<Product> findByNameContainingIgnoreCaseAndCategoryIgnoreCase(String name, String category);

    // ── NAME + BRAND ─────────────────────────────────────────
    List<Product> findByNameContainingIgnoreCaseAndBrandIgnoreCase(String name, String brand);

    // ── PRICE RANGE ──────────────────────────────────────────
    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    // ── CATEGORY + PRICE RANGE ───────────────────────────────
    List<Product> findByCategoryIgnoreCaseAndPriceBetween(String category, BigDecimal min, BigDecimal max);

    // ── IN STOCK ONLY ────────────────────────────────────────
    List<Product> findByStockGreaterThan(int stock);

    // ── PRODUCTS BY ADMIN ────────────────────────────────────
    List<Product> findByAddedById(UUID adminId);

    // ── FULL KEYWORD SEARCH (name + description + brand) ─────
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    // ── ALL DISTINCT CATEGORIES (for filter dropdowns) ───────
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findAllCategories();

    // ── ALL DISTINCT BRANDS (for filter dropdowns) ────────────
    @Query("SELECT DISTINCT p.brand FROM Product p ORDER BY p.brand")
    List<String> findAllBrands();
}