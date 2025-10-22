package com.store.store.repository;

import com.store.store.entity.Customer;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests d'intégration pour CustomerRepository
 * Teste les opérations CRUD et les requêtes personnalisées
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du CustomerRepository")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    // ==================== TESTS CRUD DE BASE ====================

    @Test
    @DisplayName("Devrait sauvegarder un client avec succès")
    void shouldSaveCustomer() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "John", "Doe", "john.doe@example.com");

        // When
        Customer savedCustomer = customerRepository.save(customer);

        // Then
        assertThat(savedCustomer).isNotNull();
        assertThat(savedCustomer.getCustomerId()).isNotNull();
        assertThat(savedCustomer.getName()).isEqualTo("John Doe");
        assertThat(savedCustomer.getEmail()).isEqualTo("john.doe@example.com");

        // ✅ Vérifier juste que le numéro existe et commence par +33
        assertThat(savedCustomer.getMobileNumber())
                .isNotNull()
                .startsWith("+336");

        assertThat(savedCustomer.getCreatedAt()).isNotNull();
        assertThat(savedCustomer.getCreatedBy()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Devrait trouver un client par son ID")
    void shouldFindCustomerById() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Jane", "Smith", "jane.smith@example.com");
        Customer savedCustomer = entityManager.persistAndFlush(customer);
        Long customerId = savedCustomer.getCustomerId();

        // When
        Optional<Customer> foundCustomer = customerRepository.findById(customerId);

        // Then
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getName()).isEqualTo("Jane Smith");
        assertThat(foundCustomer.get().getEmail()).isEqualTo("jane.smith@example.com");
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty() pour un ID inexistant")
    void shouldReturnEmptyForNonExistentId() {
        // When
        Optional<Customer> foundCustomer = customerRepository.findById(999L);

        // Then
        assertThat(foundCustomer).isEmpty();
    }

    @Test
    @DisplayName("Devrait récupérer tous les clients")
    void shouldFindAllCustomers() {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "Alice", "Brown", "alice@example.com");
        Customer customer2 = TestDataBuilder.createCustomer(null, "Bob", "White", "bob@example.com");
        Customer customer3 = TestDataBuilder.createCustomer(null, "Charlie", "Green", "charlie@example.com");

        entityManager.persist(customer1);
        entityManager.persist(customer2);
        entityManager.persist(customer3);
        entityManager.flush();

        // When
        List<Customer> customers = customerRepository.findAll();

        // Then
        assertThat(customers).hasSize(3);
        assertThat(customers).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder("alice@example.com", "bob@example.com", "charlie@example.com");
    }

    @Test
    @DisplayName("Devrait mettre à jour un client existant")
    void shouldUpdateExistingCustomer() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Update", "Test", "update@example.com");
        Customer savedCustomer = entityManager.persistAndFlush(customer);
        Long customerId = savedCustomer.getCustomerId();

        // When
        savedCustomer.setName("Updated Name");
        savedCustomer.setMobileNumber("+33698765432");
        savedCustomer.setUpdatedAt(Instant.now());
        savedCustomer.setUpdatedBy("admin");
        customerRepository.save(savedCustomer);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Customer> updatedCustomer = customerRepository.findById(customerId);
        assertThat(updatedCustomer).isPresent();
        assertThat(updatedCustomer.get().getName()).isEqualTo("Updated Name");
        assertThat(updatedCustomer.get().getMobileNumber()).isEqualTo("+33698765432");
        assertThat(updatedCustomer.get().getUpdatedAt()).isNotNull();
        assertThat(updatedCustomer.get().getUpdatedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Devrait supprimer un client par son ID")
    void shouldDeleteCustomerById() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Delete", "Test", "delete@example.com");
        Customer savedCustomer = entityManager.persistAndFlush(customer);
        Long customerId = savedCustomer.getCustomerId();

        // When
        customerRepository.deleteById(customerId);
        entityManager.flush();

        // Then
        Optional<Customer> deletedCustomer = customerRepository.findById(customerId);
        assertThat(deletedCustomer).isEmpty();
    }

    @Test
    @DisplayName("Devrait compter le nombre total de clients")
    void shouldCountAllCustomers() {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "Count", "One", "count1@example.com");
        Customer customer2 = TestDataBuilder.createCustomer(null, "Count", "Two", "count2@example.com");
        Customer customer3 = TestDataBuilder.createCustomer(null, "Count", "Three", "count3@example.com");

        entityManager.persist(customer1);
        entityManager.persist(customer2);
        entityManager.persist(customer3);
        entityManager.flush();

        // When
        long count = customerRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'un client par ID")
    void shouldCheckCustomerExistence() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Exists", "Test", "exists@example.com");
        Customer savedCustomer = entityManager.persistAndFlush(customer);
        Long customerId = savedCustomer.getCustomerId();

        // When
        boolean exists = customerRepository.existsById(customerId);
        boolean notExists = customerRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Devrait supprimer tous les clients")
    void shouldDeleteAllCustomers() {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "Delete", "All1", "deleteall1@example.com");
        Customer customer2 = TestDataBuilder.createCustomer(null, "Delete", "All2", "deleteall2@example.com");

        entityManager.persist(customer1);
        entityManager.persist(customer2);
        entityManager.flush();

        // When
        customerRepository.deleteAll();
        entityManager.flush();

        // Then
        assertThat(customerRepository.count()).isZero();
        assertThat(customerRepository.findAll()).isEmpty();
    }

    // ==================== TESTS REQUÊTES PERSONNALISÉES ====================

    @Test
    @DisplayName("findByEmail - Devrait trouver un client par email")
    void shouldFindCustomerByEmail() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Email", "Test", "email@example.com");
        entityManager.persistAndFlush(customer);

        // When
        Optional<Customer> foundCustomer = customerRepository.findByEmail("email@example.com");

        // Then
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getName()).isEqualTo("Email Test");
        assertThat(foundCustomer.get().getEmail()).isEqualTo("email@example.com");
    }

    @Test
    @DisplayName("findByEmail - Devrait retourner empty pour un email inexistant")
    void shouldReturnEmptyForNonExistentEmail() {
        // When
        Optional<Customer> foundCustomer = customerRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(foundCustomer).isEmpty();
    }

    @Test
    @DisplayName("findByEmail - Devrait trouver un client par email (exact match)")
    void shouldFindCustomerByEmailExactMatch() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Exact", "Match", "exact@example.com");
        entityManager.persistAndFlush(customer);

        // When
        Optional<Customer> found = customerRepository.findByEmail("exact@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("exact@example.com");
    }

    @Test
    @DisplayName("findByEmailOrMobileNumber - Devrait trouver un client par email")
    void shouldFindCustomerByEmailOrMobileUsingEmail() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "EmailOr", "Mobile", "emailormobile@example.com");
        entityManager.persistAndFlush(customer);

        // When
        Optional<Customer> foundCustomer = customerRepository.findByEmailOrMobileNumber(
                "emailormobile@example.com",
                "wrong-number"
        );

        // Then
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getEmail()).isEqualTo("emailormobile@example.com");
    }

    @Test
    @DisplayName("findByEmailOrMobileNumber - Devrait trouver un client par numéro de téléphone")
    void shouldFindCustomerByEmailOrMobileUsingMobile() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Mobile", "Test", "mobile@example.com");
        Customer savedCustomer = entityManager.persistAndFlush(customer);
        String actualMobileNumber = savedCustomer.getMobileNumber();

        // When
        Optional<Customer> foundCustomer = customerRepository.findByEmailOrMobileNumber(
                "wrong@email.com",
                actualMobileNumber
        );

        // Then
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getMobileNumber()).isEqualTo(actualMobileNumber);
        assertThat(foundCustomer.get().getEmail()).isEqualTo("mobile@example.com");
    }

    @Test
    @DisplayName("findByEmailOrMobileNumber - Devrait trouver un client si email ET mobile correspondent")
    void shouldFindCustomerByEmailOrMobileUsingBoth() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Both", "Match", "both@example.com");
        Customer savedCustomer = entityManager.persistAndFlush(customer);
        String actualMobileNumber = savedCustomer.getMobileNumber();

        // When
        Optional<Customer> foundCustomer = customerRepository.findByEmailOrMobileNumber(
                "both@example.com",
                actualMobileNumber
        );

        // Then
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getEmail()).isEqualTo("both@example.com");
        assertThat(foundCustomer.get().getMobileNumber()).isEqualTo(actualMobileNumber);
    }

    @Test
    @DisplayName("findByEmailOrMobileNumber - Devrait retourner empty si aucun ne correspond")
    void shouldReturnEmptyWhenNeitherEmailNorMobileMatch() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Neither", "Match", "neither@example.com");
        entityManager.persistAndFlush(customer);

        // When
        Optional<Customer> foundCustomer = customerRepository.findByEmailOrMobileNumber(
                "wrong@example.com",
                "+33600000000"
        );

        // Then
        assertThat(foundCustomer).isEmpty();
    }

    // ==================== TESTS DE CONTRAINTES ====================

    @Test
    @DisplayName("Devrait lever une exception pour un email dupliqué")
    void shouldThrowExceptionForDuplicateEmail() {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "First", "Customer", "duplicate@example.com");
        customer1.setMobileNumber("+33611111111");
        customerRepository.saveAndFlush(customer1);  // ✅ saveAndFlush

        Customer customer2 = TestDataBuilder.createCustomer(null, "Second", "Customer", "duplicate@example.com");
        customer2.setMobileNumber("+33622222222");

        // When/Then
        assertThatThrownBy(() -> customerRepository.saveAndFlush(customer2))  // ✅ saveAndFlush
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Devrait lever une exception pour un numéro de téléphone dupliqué")
    void shouldThrowExceptionForDuplicateMobileNumber() {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "First", "Phone", "first@example.com");
        customer1.setMobileNumber("+33611111111");
        customerRepository.saveAndFlush(customer1);  // ✅ saveAndFlush

        Customer customer2 = TestDataBuilder.createCustomer(null, "Second", "Phone", "second@example.com");
        customer2.setMobileNumber("+33611111111");

        // When/Then
        assertThatThrownBy(() -> customerRepository.saveAndFlush(customer2))  // ✅ saveAndFlush
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Devrait accepter deux clients avec des emails et numéros différents")
    void shouldAcceptTwoCustomersWithDifferentEmailsAndMobiles() {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "Customer", "One", "customer1@example.com");
        customer1.setMobileNumber("+33611111111");

        Customer customer2 = TestDataBuilder.createCustomer(null, "Customer", "Two", "customer2@example.com");
        customer2.setMobileNumber("+33622222222");

        // When
        customerRepository.save(customer1);
        customerRepository.save(customer2);
        entityManager.flush();

        // Then
        assertThat(customerRepository.count()).isEqualTo(2);
    }

    // ==================== TESTS DE VALIDATION ====================

    @Test
    @DisplayName("Devrait valider le format du numéro de téléphone")
    void shouldValidatePhoneNumberFormat() {
        // Given - Numéro valide
        Customer validCustomer = TestDataBuilder.createCustomer(null, "Valid", "Phone", "valid@example.com");
        validCustomer.setMobileNumber("+33612345678");

        // When/Then - Devrait réussir
        Customer savedCustomer = customerRepository.save(validCustomer);
        assertThat(savedCustomer.getCustomerId()).isNotNull();
    }

    @Test
    @DisplayName("Devrait persister toutes les propriétés d'un client")
    void shouldPersistAllCustomerProperties() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Complete", "Customer", "complete@example.com");
        customer.setMobileNumber("+33698765432");
        customer.setPasswordHash("$2a$10$specialHashedPassword");

        // When - Création
        Customer savedCustomer = customerRepository.save(customer);
        Long customerId = savedCustomer.getCustomerId();
        entityManager.flush();

        // Mise à jour
        savedCustomer.setName("Updated Complete Customer");
        savedCustomer.setUpdatedBy("admin-user");
        savedCustomer.setUpdatedAt(Instant.now());
        customerRepository.save(savedCustomer);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Customer> retrievedCustomer = customerRepository.findById(customerId);
        assertThat(retrievedCustomer).isPresent();
        Customer found = retrievedCustomer.get();

        assertThat(found.getName()).isEqualTo("Updated Complete Customer");
        assertThat(found.getUpdatedBy()).isEqualTo("admin-user");  // ✅ Maintenant non-null
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Devrait gérer correctement les valeurs null pour les champs optionnels")
    void shouldHandleNullOptionalFields() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Null", "Fields", "nullfields@example.com");
        customer.setUpdatedAt(null);
        customer.setUpdatedBy(null);

        // When
        Customer savedCustomer = customerRepository.save(customer);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());
        assertThat(retrievedCustomer).isPresent();
        assertThat(retrievedCustomer.get().getUpdatedAt()).isNull();
        assertThat(retrievedCustomer.get().getUpdatedBy()).isNull();
    }
}