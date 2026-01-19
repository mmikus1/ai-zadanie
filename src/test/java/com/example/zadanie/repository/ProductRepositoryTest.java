package com.example.zadanie.repository;

import com.example.zadanie.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveAndFindProduct() {
        // Given
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(10);

        // When
        Product saved = productRepository.save(product);
        Optional<Product> found = productRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(found.get().getStock()).isEqualTo(10);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindProductByName() {
        // Given
        Product product = new Product();
        product.setName("Unique Product");
        product.setDescription("Description");
        product.setPrice(new BigDecimal("49.99"));
        product.setStock(5);
        productRepository.save(product);

        // When
        Optional<Product> found = productRepository.findByName("Unique Product");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Unique Product");
    }

    @Test
    void shouldReturnEmptyWhenProductNotFound() {
        // When
        Optional<Product> found = productRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }
}
