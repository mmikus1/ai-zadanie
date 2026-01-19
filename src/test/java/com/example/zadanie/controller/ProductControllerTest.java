package com.example.zadanie.controller;

import com.example.zadanie.api.controller.ProductController;
import com.example.zadanie.api.dto.ProductCreateRequest;
import com.example.zadanie.api.dto.ProductUpdateRequest;
import com.example.zadanie.config.JwtAuthenticationFilter;
import com.example.zadanie.config.TestSecurityConfig;
import com.example.zadanie.entity.Product;
import com.example.zadanie.service.JwtService;
import com.example.zadanie.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
    }

    @Test
    void shouldGetAllProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(Arrays.asList(testProduct));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Test Product"))
            .andExpect(jsonPath("$[0].price").value(99.99))
            .andExpect(jsonPath("$[0].stock").value(10));
    }

    @Test
    void shouldGetProductById() throws Exception {
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Product"))
            .andExpect(jsonPath("$.price").value(99.99));
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.getProductById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
            "New Product", "Description", new BigDecimal("49.99"), 5);

        when(productService.createProduct(any())).thenReturn(testProduct);

        mockMvc.perform(post("/api/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void shouldReturnBadRequestForInvalidProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
            "", null, new BigDecimal("-10"), -5);  // Invalid data

        mockMvc.perform(post("/api/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest(
            "Updated Name", "Updated Description", new BigDecimal("79.99"), 20);

        when(productService.updateProduct(eq(1L), any())).thenReturn(testProduct);

        mockMvc.perform(put("/api/products/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentProduct() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest(
            "Updated Name", null, null, null);

        when(productService.updateProduct(eq(999L), any()))
            .thenThrow(new IllegalArgumentException("Product not found"));

        mockMvc.perform(put("/api/products/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentProduct() throws Exception {
        doThrow(new IllegalArgumentException("Product not found"))
            .when(productService).deleteProduct(999L);

        mockMvc.perform(delete("/api/products/999")
                .with(csrf()))
            .andExpect(status().isNotFound());
    }
}