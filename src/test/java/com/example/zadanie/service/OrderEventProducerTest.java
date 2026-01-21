package com.example.zadanie.service;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderEventProducer orderEventProducer;

    private Order testOrder;
    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setProduct(testProduct);
        testOrder.setQuantity(2);
        testOrder.setTotal(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void shouldPublishOrderCreatedEvent() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When
        orderEventProducer.publishOrderCreated(testOrder);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(kafkaTemplate).send(eq("order-events"), eq("order-created"), anyString());
    }

    @Test
    void shouldPublishOrderCompletedEvent() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When
        orderEventProducer.publishOrderCompleted(testOrder);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(kafkaTemplate).send(eq("order-events"), eq("order-completed"), anyString());
    }

    @Test
    void shouldPublishOrderExpiredEvent() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        // When
        orderEventProducer.publishOrderExpired(testOrder);

        // Then
        verify(objectMapper).writeValueAsString(any());
        verify(kafkaTemplate).send(eq("order-events"), eq("order-expired"), anyString());
    }

    @Test
    void shouldThrowExceptionWhenSerializationFails() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        // When & Then
        assertThatThrownBy(() -> orderEventProducer.publishOrderCreated(testOrder))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to publish order created event");
    }
}
