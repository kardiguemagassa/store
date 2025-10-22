package com.store.store.repository;

import com.store.store.constants.ApplicationConstants;
import com.store.store.entity.Contact;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour ContactRepository
 * Teste les opérations CRUD et les requêtes personnalisées
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du ContactRepository")
class ContactRepositoryTest {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        contactRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    // ==================== TESTS CRUD DE BASE ====================

    @Test
    @DisplayName("Devrait sauvegarder un contact avec succès")
    void shouldSaveContact() {
        // Given
        Contact contact = TestDataBuilder.createContact(null, "John Doe", "john@example.com",
                ApplicationConstants.OPEN_MESSAGE);

        // When
        Contact savedContact = contactRepository.save(contact);

        // Then
        assertThat(savedContact).isNotNull();
        assertThat(savedContact.getContactId()).isNotNull();
        assertThat(savedContact.getName()).isEqualTo("John Doe");
        assertThat(savedContact.getEmail()).isEqualTo("john@example.com");
        assertThat(savedContact.getStatus()).isEqualTo(ApplicationConstants.OPEN_MESSAGE);
        assertThat(savedContact.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Devrait trouver un contact par son ID")
    void shouldFindContactById() {
        // Given
        Contact contact = TestDataBuilder.createContact(null, "Jane Smith", "jane@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact savedContact = entityManager.persistAndFlush(contact);
        Long contactId = savedContact.getContactId();

        // When
        Optional<Contact> foundContact = contactRepository.findById(contactId);

        // Then
        assertThat(foundContact).isPresent();
        assertThat(foundContact.get().getName()).isEqualTo("Jane Smith");
        assertThat(foundContact.get().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty() pour un ID inexistant")
    void shouldReturnEmptyForNonExistentId() {
        // When
        Optional<Contact> foundContact = contactRepository.findById(999L);

        // Then
        assertThat(foundContact).isEmpty();
    }

    @Test
    @DisplayName("Devrait récupérer tous les contacts")
    void shouldFindAllContacts() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Contact 1", "contact1@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c2 = TestDataBuilder.createContact(null, "Contact 2", "contact2@example.com",
                ApplicationConstants.CLOSED_MESSAGE);
        Contact c3 = TestDataBuilder.createContact(null, "Contact 3", "contact3@example.com",
                ApplicationConstants.OPEN_MESSAGE);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.persist(c3);
        entityManager.flush();

        // When
        List<Contact> foundContacts = contactRepository.findAll();

        // Then
        assertThat(foundContacts).hasSize(3);
        assertThat(foundContacts).extracting(Contact::getName)
                .containsExactlyInAnyOrder("Contact 1", "Contact 2", "Contact 3");
    }

    @Test
    @DisplayName("Devrait retourner une liste vide quand aucun contact n'existe")
    void shouldReturnEmptyListWhenNoContacts() {
        // When
        List<Contact> foundContacts = contactRepository.findAll();

        // Then
        assertThat(foundContacts).isEmpty();
    }

    @Test
    @DisplayName("Devrait mettre à jour un contact existant")
    void shouldUpdateExistingContact() {
        // Given
        Contact contact = TestDataBuilder.createContact(null, "Original Name", "original@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact savedContact = entityManager.persistAndFlush(contact);
        Long contactId = savedContact.getContactId();

        // When
        savedContact.setName("Updated Name");
        savedContact.setStatus(ApplicationConstants.CLOSED_MESSAGE);
        contactRepository.save(savedContact);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Contact> updatedContact = contactRepository.findById(contactId);
        assertThat(updatedContact).isPresent();
        assertThat(updatedContact.get().getName()).isEqualTo("Updated Name");
        assertThat(updatedContact.get().getStatus()).isEqualTo(ApplicationConstants.CLOSED_MESSAGE);
    }

    @Test
    @DisplayName("Devrait supprimer un contact par son ID")
    void shouldDeleteContactById() {
        // Given
        Contact contact = TestDataBuilder.createContact(null, "To Delete", "delete@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact savedContact = entityManager.persistAndFlush(contact);
        Long contactId = savedContact.getContactId();

        // When
        contactRepository.deleteById(contactId);
        entityManager.flush();

        // Then
        Optional<Contact> deletedContact = contactRepository.findById(contactId);
        assertThat(deletedContact).isEmpty();
    }

    @Test
    @DisplayName("Devrait compter le nombre total de contacts")
    void shouldCountAllContacts() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Contact 1", "c1@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c2 = TestDataBuilder.createContact(null, "Contact 2", "c2@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c3 = TestDataBuilder.createContact(null, "Contact 3", "c3@example.com",
                ApplicationConstants.CLOSED_MESSAGE);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.persist(c3);
        entityManager.flush();

        // When
        long count = contactRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    // ==================== TESTS REQUÊTES PERSONNALISÉES ====================

    @Test
    @DisplayName("findByStatus - Devrait trouver les contacts par statut OPEN")
    void shouldFindContactsByStatusOpen() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Open 1", "open1@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c2 = TestDataBuilder.createContact(null, "Open 2", "open2@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c3 = TestDataBuilder.createContact(null, "Closed", "closed@example.com",
                ApplicationConstants.CLOSED_MESSAGE);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.persist(c3);
        entityManager.flush();

        // When
        List<Contact> openContacts = contactRepository.findByStatus(ApplicationConstants.OPEN_MESSAGE);

        // Then
        assertThat(openContacts).hasSize(2);
        assertThat(openContacts).extracting(Contact::getStatus)
                .containsOnly(ApplicationConstants.OPEN_MESSAGE);
        assertThat(openContacts).extracting(Contact::getName)
                .containsExactlyInAnyOrder("Open 1", "Open 2");
    }

    @Test
    @DisplayName("findByStatus - Devrait trouver les contacts par statut CLOSED")
    void shouldFindContactsByStatusClosed() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Open", "open@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c2 = TestDataBuilder.createContact(null, "Closed 1", "closed1@example.com",
                ApplicationConstants.CLOSED_MESSAGE);
        Contact c3 = TestDataBuilder.createContact(null, "Closed 2", "closed2@example.com",
                ApplicationConstants.CLOSED_MESSAGE);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.persist(c3);
        entityManager.flush();

        // When
        List<Contact> closedContacts = contactRepository.findByStatus(ApplicationConstants.CLOSED_MESSAGE);

        // Then
        assertThat(closedContacts).hasSize(2);
        assertThat(closedContacts).extracting(Contact::getStatus)
                .containsOnly(ApplicationConstants.CLOSED_MESSAGE);
        assertThat(closedContacts).extracting(Contact::getName)
                .containsExactlyInAnyOrder("Closed 1", "Closed 2");
    }

    @Test
    @DisplayName("findByStatus - Devrait retourner une liste vide si aucun contact avec ce statut")
    void shouldReturnEmptyListWhenNoContactsWithStatus() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Open", "open@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        entityManager.persistAndFlush(c1);

        // When
        List<Contact> closedContacts = contactRepository.findByStatus(ApplicationConstants.CLOSED_MESSAGE);

        // Then
        assertThat(closedContacts).isEmpty();
    }

    @Test
    @DisplayName("fetchByStatus - Devrait fonctionner comme findByStatus (Named Query)")
    void shouldFetchContactsByStatusUsingNamedQuery() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Open 1", "open1@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c2 = TestDataBuilder.createContact(null, "Open 2", "open2@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c3 = TestDataBuilder.createContact(null, "Closed", "closed@example.com",
                ApplicationConstants.CLOSED_MESSAGE);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.persist(c3);
        entityManager.flush();

        // When
        List<Contact> openContacts = contactRepository.fetchByStatus(ApplicationConstants.OPEN_MESSAGE);

        // Then
        assertThat(openContacts).hasSize(2);
        assertThat(openContacts).extracting(Contact::getStatus)
                .containsOnly(ApplicationConstants.OPEN_MESSAGE);
    }

    @Test
    @DisplayName("findByStatusWithNativeQuery - Devrait utiliser la requête native")
    void shouldFindContactsByStatusUsingNativeQuery() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Open", "open@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c2 = TestDataBuilder.createContact(null, "Closed 1", "closed1@example.com",
                ApplicationConstants.CLOSED_MESSAGE);
        Contact c3 = TestDataBuilder.createContact(null, "Closed 2", "closed2@example.com",
                ApplicationConstants.CLOSED_MESSAGE);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.persist(c3);
        entityManager.flush();

        // When
        List<Contact> closedContacts = contactRepository.findByStatusWithNativeQuery(
                ApplicationConstants.CLOSED_MESSAGE);

        // Then
        assertThat(closedContacts).hasSize(2);
        assertThat(closedContacts).extracting(Contact::getStatus)
                .containsOnly(ApplicationConstants.CLOSED_MESSAGE);
    }

    @Test
    @DisplayName("Devrait persister et récupérer toutes les propriétés d'un contact")
    void shouldPersistAllContactProperties() {
        // Given
        Contact contact = TestDataBuilder.createContact(null, "Complete Contact",
                "complete@example.com", ApplicationConstants.OPEN_MESSAGE);
        contact.setMobileNumber("0612345678");
        contact.setMessage("This is a complete test message");

        // When
        Contact savedContact = contactRepository.save(contact);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Contact> retrievedContact = contactRepository.findById(savedContact.getContactId());
        assertThat(retrievedContact).isPresent();
        Contact found = retrievedContact.get();

        assertThat(found.getName()).isEqualTo("Complete Contact");
        assertThat(found.getEmail()).isEqualTo("complete@example.com");
        assertThat(found.getMobileNumber()).isEqualTo("0612345678");
        assertThat(found.getMessage()).isEqualTo("This is a complete test message");
        assertThat(found.getStatus()).isEqualTo(ApplicationConstants.OPEN_MESSAGE);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getCreatedBy()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Devrait gérer correctement plusieurs statuts différents")
    void shouldHandleMultipleDifferentStatuses() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Open", "open@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c2 = TestDataBuilder.createContact(null, "Closed", "closed@example.com",
                ApplicationConstants.CLOSED_MESSAGE);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.flush();

        // When
        List<Contact> openContacts = contactRepository.findByStatus(ApplicationConstants.OPEN_MESSAGE);
        List<Contact> closedContacts = contactRepository.findByStatus(ApplicationConstants.CLOSED_MESSAGE);

        // Then
        assertThat(openContacts).hasSize(1);
        assertThat(closedContacts).hasSize(1);
        assertThat(openContacts.get(0).getName()).isEqualTo("Open");
        assertThat(closedContacts.get(0).getName()).isEqualTo("Closed");
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'un contact par ID")
    void shouldCheckContactExistence() {
        // Given
        Contact contact = TestDataBuilder.createContact(null, "Exists", "exists@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact savedContact = entityManager.persistAndFlush(contact);
        Long contactId = savedContact.getContactId();

        // When
        boolean exists = contactRepository.existsById(contactId);
        boolean notExists = contactRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Devrait supprimer tous les contacts")
    void shouldDeleteAllContacts() {
        // Given
        Contact c1 = TestDataBuilder.createContact(null, "Contact 1", "c1@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Contact c2 = TestDataBuilder.createContact(null, "Contact 2", "c2@example.com",
                ApplicationConstants.OPEN_MESSAGE);

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.flush();

        // When
        contactRepository.deleteAll();
        entityManager.flush();

        // Then
        assertThat(contactRepository.count()).isZero();
        assertThat(contactRepository.findAll()).isEmpty();
    }
}