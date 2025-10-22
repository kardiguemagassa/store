package com.store.store.util;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.*;
import com.store.store.entity.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Classe utilitaire pour créer des données de test
 * Adaptée pour travailler avec les Records Java
 */
public class TestDataBuilder {

    //CUSTOMER BUILDERS
    /**
     * Customer basique avec données par défaut valides
     */
    public static Customer createCustomer() {
        return createCustomer(null, "John Doe", "john.doe@example.com", "0612345678");
    }

    public static Customer createCustomer(Long id, String firstName, String lastName, String email) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setName(firstName + " " + lastName);
        customer.setEmail(email);
        int hashCode = Math.abs(email.hashCode() % 10000);
        customer.setMobileNumber("+3361234" + String.format("%04d", hashCode));

        customer.setPasswordHash("$2a$10$hashedPassword");
        customer.setCreatedBy(email);
        customer.setCreatedAt(Instant.now());
        return customer;
    }

    /**
     * Variante avec firstName et lastName séparés
     */
    public static Customer createCustomerWithNames(Long id, String firstName, String lastName, String email) {
        return createCustomer(id, firstName + " " + lastName, email, "0612345678");
    }

    /**
     * Crée un Customer avec adresse
     */
    public static Customer createCustomerWithAddress() {
        Customer customer = createCustomer();
        Address address = createAddress(customer);
        customer.setAddress(address);
        return customer;
    }

    /**
     * Méthode pour compatibilité avec tests de sécurité
     */
    public static Customer createCustomerForAuth(String email, String name) {
        return createCustomer(null, name, email, "0612345678");
    }

    /**
     * Crée un Customer avec des rôles
     */
    public static Customer createCustomerWithRoles(String email, Set<Role> roles) {
        Customer customer = createCustomer(null, "Test User", email, "0612345678");
        customer.setRoles(roles);
        return customer;
    }

    // ROLE BUILDERS
    public static Role createRoleEntity(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setCreatedBy("TEST_SYSTEM");
        role.setCreatedAt(Instant.now());
        return role;
    }

    // ADDRESS BUILDERS
    public static Address createAddress(Customer customer) {
        Address address = new Address();
        address.setCustomer(customer);
        address.setStreet("123 Main Street");
        address.setCity("Paris");
        address.setState("Ile-de-France");
        address.setPostalCode("75001");
        address.setCountry("FR");
        return address;
    }

    public static Address createAddress(Customer customer, String street, String city,
                                        String state, String postalCode, String country) {
        Address address = new Address();
        address.setCustomer(customer);
        address.setStreet(street);
        address.setCity(city);
        address.setState(state);
        address.setPostalCode(postalCode);
        address.setCountry(country);
        return address;
    }

    // PROFILE DTO BUILDERS
    public static ProfileRequestDto createProfileRequestDto() {
        ProfileRequestDto dto = new ProfileRequestDto();
        dto.setName("John Updated");
        dto.setEmail("john.updated@example.com");
        dto.setMobileNumber("0698765432");
        dto.setStreet("456 Updated Street");
        dto.setCity("Lyon");
        dto.setState("Auvergne-Rhône-Alpes");
        dto.setPostalCode("69001");
        dto.setCountry("FR");
        return dto;
    }

    public static ProfileRequestDto createProfileRequestDto(String name, String email,
                                                            String mobileNumber,
                                                            String street, String city, String state,
                                                            String postalCode, String country) {
        ProfileRequestDto dto = new ProfileRequestDto();
        dto.setName(name);
        dto.setEmail(email);
        dto.setMobileNumber(mobileNumber);
        dto.setStreet(street);
        dto.setCity(city);
        dto.setState(state);
        dto.setPostalCode(postalCode);
        dto.setCountry(country);
        return dto;
    }

    public static ProfileResponseDto createProfileResponseDto(Long customerId, String name,
                                                              String email, String mobileNumber,
                                                              AddressDto address) {
        ProfileResponseDto dto = new ProfileResponseDto();
        dto.setCustomerId(customerId);
        dto.setName(name);
        dto.setEmail(email);
        dto.setMobileNumber(mobileNumber);
        dto.setAddress(address);
        dto.setEmailUpdated(false);
        return dto;
    }

    // PRODUCT BUILDERS
    public static Product createProduct() {
        return createProduct(null, "Test Product", new BigDecimal("99.99"));
    }

    public static Product createProduct(Long id, String name, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("Test description for " + name);
        product.setPrice(price);
        product.setImageUrl("https://example.com/image.jpg");
        product.setPopularity(50);
        product.setCreatedAt(Instant.now());
        product.setCreatedBy("system");
        return product;
    }

    //  PRODUCT DTO BUILDERS
    public static ProductDto createProductDto(Long id, String name, BigDecimal price) {
        ProductDto dto = new ProductDto();
        dto.setProductId(id);
        dto.setName(name);
        dto.setDescription("Test description for " + name);
        dto.setPrice(price);
        dto.setPopularity(50);
        dto.setImageUrl("https://example.com/image.jpg");
        dto.setCreatedAt(Instant.now());
        return dto;
    }

    // ORDER BUILDERS
    public static Order createOrder() {
        Customer customer = createCustomer();
        return createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
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
        Order order = createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);

        Product product1 = createProduct(null, "Product 1", new BigDecimal("50.00"));
        Product product2 = createProduct(null, "Product 2", new BigDecimal("75.00"));

        OrderItem item1 = createOrderItem(null, order, product1, 2, new BigDecimal("50.00"));
        OrderItem item2 = createOrderItem(null, order, product2, 1, new BigDecimal("75.00"));

        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);
        order.setTotalPrice(new BigDecimal("175.00"));

        return order;
    }

    // ORDER ITEM BUILDERS
    public static OrderItem createOrderItem(Long id, Order order, Product product, Integer quantity, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(id);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(price);
        return orderItem;
    }

    public static OrderItem createOrderItem(Long id, Order order, Product product, int quantity, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(id);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(price);
        orderItem.setCreatedBy("test-user");
        orderItem.setCreatedAt(Instant.now());
        return orderItem;
    }

    // CONTACT BUILDERS
    public static Contact createContact() {
        return createContact(null, "Jane Smith", "jane@example.com", ApplicationConstants.OPEN_MESSAGE);
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

    // DTO BUILDERS
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
                new OrderItemReponseDto("Product 1", 2, new BigDecimal("50.00"),
                        "https://example.com/image1.jpg"),
                new OrderItemReponseDto("Product 2", 1, new BigDecimal("75.00"),
                        "https://example.com/image2.jpg")
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
        return ContactRequestDto.builder()
                .name("John Smith")
                .email("john.smith@example.com")
                .mobileNumber("0612345678")
                .message("This is a test message for contact support.")
                .build();
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

    // MÉTHOD POUR RECORDS
    public static OrderItemDto createOrderItemDto(Long productId, Integer quantity, BigDecimal price) {
        return new OrderItemDto(productId, quantity, price);
    }

    public static OrderItemReponseDto createOrderItemResponseDto(String productName, Integer quantity,
                                                                 BigDecimal price, String imageUrl) {
        return new OrderItemReponseDto(productName, quantity, price, imageUrl);
    }

    // LIST BUILDERS
    public static List<Order> createOrderList(Customer customer, int count) {
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            orders.add(createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED));
        }
        return orders;
    }

    public static List<Contact> createContactList(int count) {
        List<Contact> contacts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            contacts.add(createContact(null, "Contact " + i, "contact" + i + "@example.com",
                    ApplicationConstants.OPEN_MESSAGE));
        }
        return contacts;
    }

    public static List<Product> createProductList(int count) {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            products.add(createProduct(null, "Product " + i, new BigDecimal("50.00").multiply(new BigDecimal(i))));
        }
        return products;
    }

    public static List<OrderItemDto> createOrderItemDtoList(int count) {
        List<OrderItemDto> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(new OrderItemDto((long) i, i, new BigDecimal(i * 10.0)));
        }
        return items;
    }

    // PAYMENT BUILDERS
    public static PaymentIntentRequestDto createPaymentIntentRequestDto() {
        return new PaymentIntentRequestDto(10000L, "eur");
    }

    public static PaymentIntentRequestDto createPaymentIntentRequestDto(Long amount, String currency) {
        return new PaymentIntentRequestDto(amount, currency);
    }

    public static PaymentIntentResponseDto createPaymentIntentResponseDto(String clientSecret) {
        return new PaymentIntentResponseDto(clientSecret);
    }
}