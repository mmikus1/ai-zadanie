package com.example.zadanie.integration;

import com.example.zadanie.api.dto.ProductCreateRequest;
import com.example.zadanie.api.dto.ProductUpdateRequest;
import com.example.zadanie.entity.Product;
import com.example.zadanie.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    @Test
    @WithMockUser
    void shouldGetAllProducts() throws Exception {
        // Given
        Product product1 = new Product(null, "Laptop", "High-performance laptop", new BigDecimal("1299.99"), 10, null);
        Product product2 = new Product(null, "Mouse", "Wireless mouse", new BigDecimal("29.99"), 50, null);
        productRepository.save(product1);
        productRepository.save(product2);

        // When & Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].price").value(1299.99))
                .andExpect(jsonPath("$[0].stock").value(10))
                .andExpect(jsonPath("$[1].name").value("Mouse"))
                .andExpect(jsonPath("$[1].price").value(29.99))
                .andExpect(jsonPath("$[1].stock").value(50));
    }

    @Test
    @WithMockUser
    void shouldGetProductById() throws Exception {
        // Given
        Product product = new Product(null, "Laptop", "High-performance laptop", new BigDecimal("1299.99"), 10, null);
        Product savedProduct = productRepository.save(product);

        // When & Then
        mockMvc.perform(get("/api/products/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.description").value("High-performance laptop"))
                .andExpect(jsonPath("$.price").value(1299.99))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldCreateProductWithValidData() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest(
                "Laptop",
                "High-performance laptop",
                new BigDecimal("1299.99"),
                10
        );

        // When
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.description").value("High-performance laptop"))
                .andExpect(jsonPath("$.price").value(1299.99))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        // Then - Verify it's persisted in the database
        assertThat(productRepository.count()).isEqualTo(1);
        Product savedProduct = productRepository.findAll().get(0);
        assertThat(savedProduct.getName()).isEqualTo("Laptop");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("1299.99"));
        assertThat(savedProduct.getStock()).isEqualTo(10);
    }

    @Test
    @WithMockUser
    void shouldCreateProductWithoutDescription() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest(
                "Mouse",
                null,
                new BigDecimal("29.99"),
                50
        );

        // When & Then
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.stock").value(50));
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForNegativePrice() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest(
                "Laptop",
                "High-performance laptop",
                new BigDecimal("-100"),
                10
        );

        // When & Then
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(productRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForNegativeStock() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest(
                "Laptop",
                "High-performance laptop",
                new BigDecimal("1299.99"),
                -5
        );

        // When & Then
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(productRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForMissingName() throws Exception {
        // Given
        String requestJson = "{\"description\":\"Test\",\"price\":100,\"stock\":10}";

        // When & Then
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(productRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldUpdateProduct() throws Exception {
        // Given
        Product product = new Product(null, "Laptop", "Old description", new BigDecimal("1299.99"), 10, null);
        Product savedProduct = productRepository.save(product);

        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "Gaming Laptop",
                "Updated high-performance gaming laptop",
                new BigDecimal("1599.99"),
                15
        );

        // When
        mockMvc.perform(put("/api/products/" + savedProduct.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.description").value("Updated high-performance gaming laptop"))
                .andExpect(jsonPath("$.price").value(1599.99))
                .andExpect(jsonPath("$.stock").value(15));

        // Then - Verify it's updated in the database
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getName()).isEqualTo("Gaming Laptop");
        assertThat(updatedProduct.getDescription()).isEqualTo("Updated high-performance gaming laptop");
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("1599.99"));
        assertThat(updatedProduct.getStock()).isEqualTo(15);
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenUpdatingNonExistentProduct() throws Exception {
        // Given
        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "Gaming Laptop",
                "Updated description",
                new BigDecimal("1599.99"),
                15
        );

        // When & Then
        mockMvc.perform(put("/api/products/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldDeleteProduct() throws Exception {
        // Given
        Product product = new Product(null, "Laptop", "High-performance laptop", new BigDecimal("1299.99"), 10, null);
        Product savedProduct = productRepository.save(product);

        // When
        mockMvc.perform(delete("/api/products/" + savedProduct.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Then - Verify it's deleted from the database
        assertThat(productRepository.findById(savedProduct.getId())).isEmpty();
        assertThat(productRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenDeletingNonExistentProduct() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/products/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoProducts() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void shouldAllowZeroPrice() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest(
                "Free Item",
                "This is free",
                BigDecimal.ZERO,
                100
        );

        // When & Then
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(0));
    }

    @Test
    @WithMockUser
    void shouldAllowZeroStock() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest(
                "Out of Stock",
                "Currently unavailable",
                new BigDecimal("99.99"),
                0
        );

        // When & Then
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value(0));
    }
}