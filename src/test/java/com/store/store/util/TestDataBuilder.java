package com.store.store.util;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.*;
import com.store.store.entity.*;
import com.store.store.enums.RoleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ✅ Classe utilitaire pour créer des données de test
 * Mise à jour avec toutes les corrections :
 * - paymentIntentId au lieu de paymentId
 * - OrderResponseDto avec builder et tous les champs
 * - OrderItemResponseDto avec builder et tous les champs
 * - Support complet des DTOs corrigés
 */
public class TestDataBuilder {

    // =====================================================
    // CATEGORY BUILDERS
    // =====================================================

    /**
     * Crée une catégorie pour les tests
     */
    public static Category createCategory(Long id, String code, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCode(code);
        category.setName(name);
        category.setDescription("Description de test pour " + name);
        category.setIcon("🧪");
        category.setDisplayOrder(1);
        category.setIsActive(true);
        return category;
    }

    /**
     * Catégorie par défaut pour les tests
     */
    public static Category createDefaultCategory() {
        return createCategory(1L, "TEST_CATEGORY", "Test Category");
    }

    /**
     * Catégorie SPORTS pour les tests
     */
    public static Category createSportsCategory() {
        return createCategory(1L, "SPORTS", "Sports");
    }

    /**
     * Catégorie ANIME pour les tests
     */
    public static Category createAnimeCategory() {
        return createCategory(2L, "ANIME", "Anime & Manga");
    }

    /**
     * Catégorie CODING pour les tests
     */
    public static Category createCodingCategory() {
        return createCategory(3L, "CODING", "Code & Tech");
    }

    // =====================================================
    // PRODUCT ENTITY BUILDERS
    // =====================================================

    /**
     * ✅ Crée un produit avec une catégorie
     */
    public static Product createProduct(Long id, String name, BigDecimal price, Category category) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("Description de test pour " + name);
        product.setPrice(price);
        product.setPopularity(50);
        product.setImageUrl("/images/test-product.png");
        product.setCategory(category);
        return product;
    }

    /**
     * ✅ Crée un produit avec catégorie par défaut
     */
    public static Product createProduct(Long id, String name, BigDecimal price) {
        return createProduct(id, name, price, createDefaultCategory());
    }

    /**
     * ✅ Crée un produit de test standard
     */
    public static Product createTestProduct() {
        return createProduct(1L, "Test Product", new BigDecimal("49.99"), createDefaultCategory());
    }

    /**
     * ✅ Crée plusieurs produits de test
     */
    public static List<Product> createMultipleProducts(int count) {
        Category category = createDefaultCategory();
        return java.util.stream.IntStream.range(1, count + 1)
                .mapToObj(i -> createProduct(
                        (long) i,
                        "Product " + i,
                        BigDecimal.valueOf(10.00 * i),
                        category
                ))
                .toList();
    }

    /**
     * Crée une liste de produits
     */
    public static List<Product> createProductList(int count) {
        List<Product> products = new ArrayList<>();
        Category category = createDefaultCategory();
        for (int i = 1; i <= count; i++) {
            products.add(createProduct(
                    null,
                    "Product " + i,
                    new BigDecimal("50.00").multiply(new BigDecimal(i)),
                    category
            ));
        }
        return products;
    }

    // =====================================================
    // PRODUCT DTO BUILDERS
    // =====================================================

    /**
     * Crée un ProductDto pour les tests
     */
    public static ProductDto createProductDto(Long id, String name, BigDecimal price) {
        ProductDto dto = new ProductDto();
        dto.setProductId(id);
        dto.setName(name);
        dto.setDescription("Test description for " + name);
        dto.setPrice(price);
        dto.setPopularity(50);
        dto.setImageUrl("https://example.com/image.jpg");
        return dto;
    }

    // =====================================================
    // ORDER REQUEST DTO BUILDERS
    // =====================================================

    /**
     * ✅ Crée une requête de commande valide
     */
    public static OrderRequestDto createValidOrderRequest() {
        return new OrderRequestDto(
                new BigDecimal("99.98"),  // 49.99 * 2
                "pi_test_12345",
                "paid",
                List.of(new OrderItemDto(1L, 2, new BigDecimal("49.99")))
        );
    }

    /**
     * ✅ Crée une requête de commande avec produit spécifique
     */
    public static OrderRequestDto createOrderRequest(Long productId, int quantity, BigDecimal price) {
        BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));
        return new OrderRequestDto(
                totalPrice,
                "pi_test_" + System.currentTimeMillis(),
                "paid",
                List.of(new OrderItemDto(productId, quantity, price))
        );
    }

    /**
     * ✅ Crée une requête de commande avec plusieurs produits
     */
    public static OrderRequestDto createValidOrderRequest(Long... productIds) {
        List<OrderItemDto> items = Arrays.stream(productIds)
                .map(id -> new OrderItemDto(id, 1, new BigDecimal("99.99")))
                .toList();

        BigDecimal total = new BigDecimal("99.99").multiply(new BigDecimal(items.size()));

        return new OrderRequestDto(
                total,
                "pi_test_" + UUID.randomUUID(),
                "paid",
                items
        );
    }

    /**
     * ✅ Crée une requête de commande avec items personnalisés
     */
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

    /**
     * ✅ Crée une requête de commande invalide (pour tests négatifs)
     */
    public static OrderRequestDto createInvalidOrderRequest() {
        return new OrderRequestDto(
                BigDecimal.ZERO,
                null,
                null,
                List.of()
        );
    }

    // =====================================================
    // ORDER RESPONSE DTO BUILDERS (CORRIGÉS) ✅
    // =====================================================

    /**
     * ✅ Crée une réponse de commande simple
     */
    public static OrderResponseDto createOrderResponse(Long orderId) {
        return OrderResponseDto.builder()
                .orderId(orderId)
                .orderStatus("CREATED")
                .totalPrice(new BigDecimal("99.99"))
                .paymentIntentId("pi_test_12345")
                .paymentStatus("paid")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(List.of(createOrderItemResponse(1L)))
                .build();
    }

    /**
     * ✅ Crée une réponse de commande avec statut personnalisé
     */
    public static OrderResponseDto createOrderResponse(Long orderId, String orderStatus) {
        return OrderResponseDto.builder()
                .orderId(orderId)
                .orderStatus(orderStatus)
                .totalPrice(new BigDecimal("99.99"))
                .paymentIntentId("pi_test_" + orderId)
                .paymentStatus("paid")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(List.of(createOrderItemResponse(1L)))
                .build();
    }

    /**
     * ✅ Crée une réponse de commande complète
     */
    public static OrderResponseDto createOrderResponseDto(Long orderId) {
        List<OrderItemResponseDto> items = List.of(
                createOrderItemResponse(1L, "Product 1", 2, new BigDecimal("50.00")),
                createOrderItemResponse(2L, "Product 2", 1, new BigDecimal("75.00"))
        );

        return OrderResponseDto.builder()
                .orderId(orderId)
                .orderStatus(ApplicationConstants.ORDER_STATUS_CREATED)
                .totalPrice(new BigDecimal("175.00"))
                .paymentIntentId("pi_test_123456")
                .paymentStatus("paid")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(items)
                .build();
    }

    /**
     * ✅ Commande en attente
     */
    public static OrderResponseDto createPendingOrderResponse(Long orderId) {
        return createOrderResponse(orderId, "CREATED");
    }

    /**
     * ✅ Commande confirmée
     */
    public static OrderResponseDto createConfirmedOrderResponse(Long orderId) {
        return createOrderResponse(orderId, "CONFIRMED");
    }

    /**
     * ✅ Commande annulée
     */
    public static OrderResponseDto createCancelledOrderResponse(Long orderId) {
        return createOrderResponse(orderId, "CANCELLED");
    }

    // =====================================================
    // ORDER ITEM DTO BUILDERS (CORRIGÉS) ✅
    // =====================================================

    /**
     * ✅ Crée un OrderItemDto
     */
    public static OrderItemDto createOrderItemDto(Long productId, Integer quantity, BigDecimal price) {
        return new OrderItemDto(productId, quantity, price);
    }

    /**
     * ✅ Crée une liste d'OrderItemDto
     */
    public static List<OrderItemDto> createOrderItemDtoList(int count) {
        List<OrderItemDto> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(new OrderItemDto((long) i, i, new BigDecimal(i * 10.0)));
        }
        return items;
    }

    // =====================================================
    // ORDER ITEM RESPONSE DTO BUILDERS (CORRIGÉS) ✅
    // =====================================================

    /**
     * ✅ Crée un OrderItemResponseDto simple
     */
    public static OrderItemResponseDto createOrderItemResponse(Long productId) {
        return OrderItemResponseDto.builder()
                .orderItemId(1L)
                .productId(productId)
                .productName("Test Product")
                .productImageUrl("/images/test-product.png")
                .quantity(1)
                .price(new BigDecimal("49.99"))
                .subtotal(new BigDecimal("49.99"))
                .build();
    }

    /**
     * ✅ Crée un OrderItemResponseDto complet
     */
    public static OrderItemResponseDto createOrderItemResponse(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal price) {

        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

        return OrderItemResponseDto.builder()
                .orderItemId(productId)
                .productId(productId)
                .productName(productName)
                .productImageUrl("https://example.com/image" + productId + ".jpg")
                .quantity(quantity)
                .price(price)
                .subtotal(subtotal)
                .build();
    }

    /**
     * ✅ Version avec paramètres limités (rétrocompatibilité)
     */
    public static OrderItemResponseDto createOrderItemResponseDto(
            String productName,
            Integer quantity,
            BigDecimal price,
            String imageUrl) {

        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

        return OrderItemResponseDto.builder()
                .orderItemId(1L)
                .productId(1L)
                .productName(productName)
                .productImageUrl(imageUrl)
                .quantity(quantity)
                .price(price)
                .subtotal(subtotal)
                .build();
    }

    // =====================================================
    // ORDER ENTITY BUILDERS (CORRIGÉS) ✅
    // =====================================================

    /**
     * ✅ Crée une commande basique
     */
    public static Order createOrder() {
        Customer customer = createCustomer();
        return createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
    }

    /**
     * ✅ Crée une commande avec paramètres
     */
    public static Order createOrder(Long id, Customer customer, String status) {
        Order order = new Order();
        order.setOrderId(id);
        order.setCustomer(customer);
        order.setOrderStatus(status);
        order.setTotalPrice(new BigDecimal("199.99"));
        order.setPaymentIntentId("pi_test_123456");  // ✅ Corrigé
        order.setPaymentStatus("paid");
        order.setCreatedAt(Instant.now());
        order.setCreatedBy(customer.getEmail());
        return order;
    }

    /**
     * ✅ Crée une commande avec items
     */
    public static Order createOrderWithItems(Customer customer) {
        Order order = createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);

        Product product1 = createProduct(null, "Product 1", new BigDecimal("50.00"));
        Product product2 = createProduct(null, "Product 2", new BigDecimal("75.00"));

        OrderItem item1 = createOrderItem(null, order, product1, 2, new BigDecimal("50.00"));
        OrderItem item2 = createOrderItem(null, order, product2, 1, new BigDecimal("75.00"));

        // ✅ Utiliser addOrderItem si disponible
        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);
        order.setTotalPrice(new BigDecimal("175.00"));

        return order;
    }

    /**
     * Crée une liste de commandes
     */
    public static List<Order> createOrderList(Customer customer, int count) {
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            orders.add(createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED));
        }
        return orders;
    }

    // =====================================================
    // ORDER ITEM ENTITY BUILDERS
    // =====================================================

    /**
     * Crée un OrderItem
     */
    public static OrderItem createOrderItem(Long id, Order order, Product product, Integer quantity, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(id);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(price);
        return orderItem;
    }

    /**
     * Crée un OrderItem avec dates
     */
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

    // =====================================================
    // CUSTOMER BUILDERS
    // =====================================================

    /**
     * Customer basique avec données par défaut
     */
    public static Customer createCustomer() {
        return createCustomer(null, "John", "Doe", "john.doe@example.com");
    }

    /**
     * Customer avec paramètres
     */
    public static Customer createCustomer(Long id, String firstName, String lastName, String email) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setName(firstName + " " + lastName);
        customer.setEmail(email);

        // Générer un numéro de téléphone basé sur l'email
        int hashCode = Math.abs(email.hashCode() % 10000);
        customer.setMobileNumber("+3361234" + String.format("%04d", hashCode));

        customer.setPasswordHash("$2a$10$hashedPassword");
        customer.setCreatedBy(email);
        customer.setCreatedAt(Instant.now());
        return customer;
    }

    /**
     * Customer avec adresse
     */
    public static Customer createCustomerWithAddress() {
        Customer customer = createCustomer();
        Address address = createAddress(customer);
        customer.setAddress(address);
        return customer;
    }

    /**
     * Customer pour auth
     */
    public static Customer createCustomerForAuth(String email, String name) {
        String[] parts = name.split(" ", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";
        return createCustomer(null, firstName, lastName, email);
    }

    /**
     * Customer avec rôles
     */
    public static Customer createCustomerWithRoles(String email, Set<Role> roles) {
        Customer customer = createCustomer(null, "Test", "User", email);
        customer.setRoles(roles);
        return customer;
    }

    // =====================================================
    // ROLE BUILDERS
    // =====================================================

    public static Role createRole(Long id, String roleName) {
        Role role = new Role();
        role.setRoleId(id);
        //role.setRoleName(roleName);
        //role.setName(roleName);
        role.setName(RoleType.ROLE_ADMIN);
        return role;
    }

    public static Role createUserRole() {
        return createRole(1L, ApplicationConstants.ROLE_USER);
    }

    public static Role createAdminRole() {
        return createRole(2L, ApplicationConstants.ROLE_ADMIN);
    }

    public static Set<Role> createUserRoleSet() {
        return new HashSet<>(Set.of(createUserRole()));
    }

    public static Set<Role> createAdminRoleSet() {
        return new HashSet<>(Set.of(createUserRole(), createAdminRole()));
    }

    // =====================================================
    // ADDRESS BUILDERS
    // =====================================================

    public static Address createAddress(Customer customer) {
        Address address = new Address();
        address.setCustomer(customer);
        address.setStreet("123 Test Street");
        address.setCity("Paris");
        address.setState("Île-de-France");
        address.setPostalCode("75001");
        address.setCountry("France");
        return address;
    }

    public static AddressDto createAddressDto(String street, String city, String state, String postalCode, String country) {
        AddressDto dto = new AddressDto();
        dto.setStreet(street);
        dto.setCity(city);
        dto.setState(state);
        dto.setPostalCode(postalCode);
        dto.setCountry(country);
        return dto;
    }

    // =====================================================
    // PROFILE DTO BUILDERS
    // =====================================================

    public static ProfileRequestDto createProfileRequestDto(String name, String email, String mobileNumber,
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

    // =====================================================
    // CONTACT BUILDERS
    // =====================================================

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

    public static List<Contact> createContactList(int count) {
        List<Contact> contacts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            contacts.add(createContact(
                    null,
                    "Contact " + i,
                    "contact" + i + "@example.com",
                    ApplicationConstants.OPEN_MESSAGE
            ));
        }
        return contacts;
    }

    // =====================================================
    // CONTACT DTO BUILDERS
    // =====================================================

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

    // =====================================================
    // PAYMENT DTO BUILDERS
    // =====================================================

    /**
     * Crée un PaymentIntentRequestDto par défaut
     */
    public static PaymentIntentRequestDto createPaymentIntentRequestDto() {
        return new PaymentIntentRequestDto(10000L, "eur");
    }

    /**
     * Crée un PaymentIntentRequestDto avec montant et devise
     */
    public static PaymentIntentRequestDto createPaymentIntentRequestDto(Long amount, String currency) {
        return new PaymentIntentRequestDto(amount, currency);
    }

    /**
     * Crée un PaymentIntentResponseDto
     */
    public static PaymentIntentResponseDto createPaymentIntentResponseDto(String clientSecret) {
        return new PaymentIntentResponseDto(clientSecret);
    }
}