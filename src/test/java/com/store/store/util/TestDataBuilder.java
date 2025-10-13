package com.store.store.util;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.*;
import com.store.store.entity.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour créer des données de test
 * Pattern Builder pour faciliter la création d'objets complexes dans les tests
 */
public class TestDataBuilder {

    // ==================== CUSTOMER BUILDERS ====================

    public static Customer createCustomer() {
        return createCustomer(1L, "John", "Doe", "john.doe@example.com");
    }

    public static Customer createCustomer(Long id, String firstName, String lastName, String email) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        /*customer.setFirstName(firstName);
        customer.setLastName(lastName);*/
        customer.setName(firstName);
        customer.setEmail(email);
        customer.setMobileNumber("0612345678");
        customer.setCreatedAt(Instant.now());
        customer.setCreatedBy("system");
        return customer;
    }

    // ==================== PRODUCT BUILDERS ====================

    public static Product createProduct() {
        return createProduct(1L, "Test Product", new BigDecimal("99.99"));
    }

    public static Product createProduct(Long id, String name, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("Test description for " + name);
        product.setPrice(price);
        product.setImageUrl("https://example.com/image.jpg");
        product.setCreatedAt(Instant.now());
        product.setCreatedBy("system");
        return product;
    }

    // ==================== ORDER BUILDERS ====================

    public static Order createOrder() {
        return createOrder(1L, createCustomer(), ApplicationConstants.ORDER_STATUS_CREATED);
    }

    public static Order createOrder(Long id, Customer customer, String status) {
        Order order = new Order();
        order.setOrderId(id);
        order.setCustomer(customer);
        order.setOrderStatus(status);
        order.setTotalPrice(new BigDecimal("199.99"));
        order.setPaymentId("pi_test_123456");
        order.setPaymentStatus("paid");
        order.setOrderItems(new ArrayList<>());
        order.setCreatedAt(Instant.now());
        order.setCreatedBy(customer.getEmail());
        return order;
    }

    public static Order createOrderWithItems(Customer customer) {
        Order order = createOrder(1L, customer, ApplicationConstants.ORDER_STATUS_CREATED);

        Product product1 = createProduct(1L, "Product 1", new BigDecimal("50.00"));
        Product product2 = createProduct(2L, "Product 2", new BigDecimal("75.00"));

        OrderItem item1 = createOrderItem(1L, order, product1, 2, new BigDecimal("50.00"));
        OrderItem item2 = createOrderItem(2L, order, product2, 1, new BigDecimal("75.00"));

        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);
        order.setTotalPrice(new BigDecimal("175.00"));

        return order;
    }

    // ==================== ORDER ITEM BUILDERS ====================

    public static OrderItem createOrderItem(Long id, Order order, Product product, Integer quantity, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(id);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(price);
        return orderItem;
    }

    // ==================== CONTACT BUILDERS ====================

    public static Contact createContact() {
        return createContact(1L, "Jane Smith", "jane@example.com", ApplicationConstants.OPEN_MESSAGE);
    }

    public static Contact createContact(Long id, String name, String email, String status) {
        Contact contact = new Contact();
        contact.setContactId(id);
        contact.setName(name);
        contact.setEmail(email);
        contact.setMobileNumber("0698765432");
        contact.setMessage("Test message from " + name);
        contact.setStatus(status);
        contact.setCreatedAt(Instant.now());
        contact.setCreatedBy("anonymous");
        return contact;
    }

    // ==================== DTO BUILDERS ====================

    public static OrderRequestDto createOrderRequestDto() {
        List<OrderItemDto> items = List.of(
                new OrderItemDto(1L, 2, new BigDecimal("50.00")),
                new OrderItemDto(2L, 1, new BigDecimal("75.00"))
        );

        return new OrderRequestDto(
                new BigDecimal("175.00"),
                "pi_test_123456",
                "paid",
                items
        );
    }

    public static OrderResponseDto createOrderResponseDto(Long orderId) {
        List<OrderItemReponseDto> items = List.of(
                new OrderItemReponseDto("Product 1", 2, new BigDecimal("50.00"), "https://example.com/image1.jpg"),
                new OrderItemReponseDto("Product 2", 1, new BigDecimal("75.00"), "https://example.com/image2.jpg")
        );

        return new OrderResponseDto(
                orderId,
                ApplicationConstants.ORDER_STATUS_CREATED,
                new BigDecimal("175.00"),
                Instant.now().toString(),
                items
        );
    }

    public static ContactRequestDto createContactRequestDto() {
        ContactRequestDto dto = new ContactRequestDto();
        dto.setName("John Smith");
        dto.setEmail("john.smith@example.com");
        dto.setMobileNumber("0612345678");
        dto.setMessage("This is a test message for contact support.");
        return dto;
    }

    public static ContactResponseDto createContactResponseDto(Long contactId) {
        return new ContactResponseDto(
                contactId,
                "John Smith",
                "john.smith@example.com",
                "0612345678",
                "This is a test message for contact support.",
                ApplicationConstants.OPEN_MESSAGE
        );
    }

    // ==================== LIST BUILDERS ====================

    public static List<Order> createOrderList(Customer customer, int count) {
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            orders.add(createOrder((long) i, customer, ApplicationConstants.ORDER_STATUS_CREATED));
        }
        return orders;
    }

    public static List<Contact> createContactList(int count) {
        List<Contact> contacts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            contacts.add(createContact((long) i, "Contact " + i, "contact" + i + "@example.com",
                    ApplicationConstants.OPEN_MESSAGE));
        }
        return contacts;
    }

    public static List<Product> createProductList(int count) {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            products.add(createProduct((long) i, "Product " + i, new BigDecimal("50.00").multiply(new BigDecimal(i))));
        }
        return products;
    }
}