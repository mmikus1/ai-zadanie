package com.example.zadanie.service;

import com.example.zadanie.api.dto.ProductCreateRequest;
import com.example.zadanie.api.dto.ProductUpdateRequest;
import com.example.zadanie.entity.Product;
import com.example.zadanie.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

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
    void shouldGetAllProducts() {
        // Given
        when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

        // When
        List<Product> products = productService.getAllProducts();

        // Then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Test Product");
        verify(productRepository).findAll();
    }

    @Test
    void shouldGetProductById() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        Optional<Product> found = productService.getProductById(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
        verify(productRepository).findById(1L);
    }

    @Test
    void shouldCreateProduct() {
        // Given
        ProductCreateRequest request = new ProductCreateRequest(
            "New Product", "Description", new BigDecimal("49.99"), 5);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product created = productService.createProduct(request);

        // Then
        assertThat(created).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldUpdateProduct() {
        // Given
        ProductUpdateRequest request = new ProductUpdateRequest(
            "Updated Name", "Updated Description", new BigDecimal("79.99"), 20);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product updated = productService.updateProduct(1L, request);

        // Then
        assertThat(updated).isNotNull();
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        // Given
        ProductUpdateRequest request = new ProductUpdateRequest(
            "Updated Name", null, null, null);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> productService.updateProduct(999L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    void shouldDeleteProduct() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentProduct() {
        // Given
        when(productRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> productService.deleteProduct(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product not found");
    }
}