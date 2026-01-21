package com.example.zadanie.integration;

import com.example.zadanie.entity.Order;
import com.example.zadanie.entity.OrderStatus;
import com.example.zadanie.entity.Product;
import com.example.zadanie.entity.User;
import com.example.zadanie.event.OrderCreatedEvent;
import com.example.zadanie.repository.OrderRepository;
import com.example.zadanie.repository.ProductRepository;
import com.example.zadanie.repository.UserRepository;
import com.example.zadanie.service.OrderEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(topics = {"order-events"}, partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"})
@DirtiesContext
class OrderKafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaMessageListenerContainer<String, String> container;
    private BlockingQueue<ConsumerRecord<String, String>> records;

    private User testUser;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Clean up
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // Set up Kafka consumer for testing
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties("order-events");

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, String>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // Clear any existing messages from the queue
        records.clear();

        // Create test data
        testUser = new User(null, "Test User", "test@example.com", "password");
        testUser = userRepository.save(testUser);

        testProduct = new Product(null, "Test Product", "Description", new BigDecimal("99.99"), 10, null);
        testProduct = productRepository.save(testProduct);

        testOrder = new Order(null, testUser, testProduct, 2, new BigDecimal("199.98"), OrderStatus.PENDING, null, null);
        testOrder = orderRepository.save(testOrder);
    }

    @AfterEach
    void tearDown() {
        container.stop();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldPublishOrderCreatedEventToKafka() throws Exception {
        // When
        orderEventProducer.publishOrderCreated(testOrder);

        // Then
        ConsumerRecord<String, String> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("order-created");

        String json = record.value();
        OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);

        assertThat(event.getOrderId()).isEqualTo(testOrder.getId());
        assertThat(event.getUserId()).isEqualTo(testUser.getId());
        assertThat(event.getProductId()).isEqualTo(testProduct.getId());
        assertThat(event.getQuantity()).isEqualTo(2);
        assertThat(event.getTotal()).isEqualByComparingTo(new BigDecimal("199.98"));
        assertThat(event.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldPublishOrderCompletedEventToKafka() throws Exception {
        // Given
        testOrder.setStatus(OrderStatus.COMPLETED);
        testOrder = orderRepository.save(testOrder);

        // When
        orderEventProducer.publishOrderCompleted(testOrder);

        // Then
        ConsumerRecord<String, String> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("order-completed");

        String json = record.value();
        assertThat(json).contains("\"orderId\":" + testOrder.getId());
        assertThat(json).contains("\"userId\":" + testUser.getId());
    }

    @Test
    void shouldPublishOrderExpiredEventToKafka() throws Exception {
        // Given
        testOrder.setStatus(OrderStatus.EXPIRED);
        testOrder = orderRepository.save(testOrder);

        // When
        orderEventProducer.publishOrderExpired(testOrder);

        // Then
        ConsumerRecord<String, String> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("order-expired");

        String json = record.value();
        assertThat(json).contains("\"orderId\":" + testOrder.getId());
    }
}
