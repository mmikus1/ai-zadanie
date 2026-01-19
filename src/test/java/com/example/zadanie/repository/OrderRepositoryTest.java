package com.example.zadanie.repository;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void shouldSaveAndFindOrder() {
        // Given
        Order order = new Order();
        order.setUser(testUser);
        order.setProduct(testProduct);
        order.setQuantity(2);
        order.setTotal(new BigDecimal("199.98"));
        order.setStatus(OrderStatus.PENDING);

        // When
        Order saved = orderRepository.save(order);
        Optional<Order> found = orderRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(2);
        assertThat(found.get().getTotal()).isEqualByComparingTo(new BigDecimal("199.98"));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindOrdersByUserId() {
        // Given
        Order order1 = new Order();
        order1.setUser(testUser);
        order1.setProduct(testProduct);
        order1.setQuantity(1);
        order1.setTotal(new BigDecimal("99.99"));
        order1.setStatus(OrderStatus.PENDING);
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setUser(testUser);
        order2.setProduct(testProduct);
        order2.setQuantity(2);
        order2.setTotal(new BigDecimal("199.98"));
        order2.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order2);

        // When
        List<Order> orders = orderRepository.findByUserId(testUser.getId());

        // Then
        assertThat(orders).hasSize(2);
    }

    @Test
    void shouldFindOrdersByStatus() {
        // Given
        Order order1 = new Order();
        order1.setUser(testUser);
        order1.setProduct(testProduct);
        order1.setQuantity(1);
        order1.setTotal(new BigDecimal("99.99"));
        order1.setStatus(OrderStatus.PENDING);
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setUser(testUser);
        order2.setProduct(testProduct);
        order2.setQuantity(2);
        order2.setTotal(new BigDecimal("199.98"));
        order2.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order2);

        // When
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);

        // Then
        assertThat(pendingOrders).hasSize(1);
        assertThat(completedOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        // When
        Optional<Order> found = orderRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }
}
