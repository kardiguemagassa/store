/*package com.store.store.repository;

import com.store.store.constants.ApplicationConstants;
import com.store.store.entity.Contact;
import com.store.store.repository.ContactRepository;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du Repository - ContactRepository")
class ContactRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    @DisplayName("findByStatus - Devrait retourner les contacts avec le statut spécifié")
    void findByStatus_ShouldReturnContactsWithGivenStatus() {
        // Arrange
        Contact contact1 = createAndPersistContact("John Doe", "john@example.com", ApplicationConstants.OPEN_MESSAGE);
        Contact contact2 = createAndPersistContact("Jane Smith", "jane@example.com", ApplicationConstants.OPEN_MESSAGE);
        Contact contact3 = createAndPersistContact("Bob Wilson", "bob@example.com", ApplicationConstants.CLOSED_MESSAGE);

        // Act
        List<Contact> openContacts = contactRepository.findByStatus(ApplicationConstants.OPEN_MESSAGE);
        List<Contact> closedContacts = contactRepository.findByStatus(ApplicationConstants.CLOSED_MESSAGE);

        // Assert
        assertThat(openContacts).hasSize(2);
        assertThat(openContacts).extracting(Contact::getContactId)
                .containsExactlyInAnyOrder(contact1.getContactId(), contact2.getContactId());

        assertThat(closedContacts).hasSize(1);
        assertThat(closedContacts.get(0).getContactId()).isEqualTo(contact3.getContactId());
    }

    @Test
    @DisplayName("findByStatus - Devrait retourner liste vide si aucun contact avec ce statut")
    void findByStatus_ShouldReturnEmptyList_WhenNoContactsWithStatus() {
        // Arrange
        createAndPersistContact("John Doe", "john@example.com", ApplicationConstants.OPEN_MESSAGE);

        // Act
        List<Contact> closedContacts = contactRepository.findByStatus(ApplicationConstants.CLOSED_MESSAGE);

        // Assert
        assertThat(closedContacts).isEmpty();
    }

    @Test
    @DisplayName("fetchByStatus (Named Query) - Devrait retourner les contacts avec le statut")
    void fetchByStatus_NamedQuery_ShouldReturnContactsWithStatus() {
        // Arrange
        createAndPersistContact("Alice Brown", "alice@example.com", ApplicationConstants.OPEN_MESSAGE);
        createAndPersistContact("Charlie Davis", "charlie@example.com", ApplicationConstants.OPEN_MESSAGE);
        createAndPersistContact("Diana Evans", "diana@example.com", ApplicationConstants.CLOSED_MESSAGE);

        // Act
        List<Contact> openContacts = contactRepository.fetchByStatus(ApplicationConstants.OPEN_MESSAGE);

        // Assert
        assertThat(openContacts).hasSize(2);
        assertThat(openContacts).allMatch(c -> c.getStatus().equals(ApplicationConstants.OPEN_MESSAGE));
        assertThat(openContacts).extracting(Contact::getName)
                .containsExactlyInAnyOrder("Alice Brown", "Charlie Davis");
    }

    @Test
    @DisplayName("findByStatusWithNativeQuery - Devrait retourner les contacts avec le statut")
    void findByStatusWithNativeQuery_ShouldReturnContactsWithStatus() {
        // Arrange
        createAndPersistContact("Mark Johnson", "mark@example.com", ApplicationConstants.OPEN_MESSAGE);
        createAndPersistContact("Sarah Lee", "sarah@example.com", ApplicationConstants.CLOSED_MESSAGE);
        createAndPersistContact("Tom Anderson", "tom@example.com", ApplicationConstants.OPEN_MESSAGE);

        // Act
        List<Contact> openContacts = contactRepository.findByStatusWithNativeQuery(ApplicationConstants.OPEN_MESSAGE);

        // Assert
        assertThat(openContacts).hasSize(2);
        assertThat(openContacts).allMatch(c -> c.getStatus().equals(ApplicationConstants.OPEN_MESSAGE));
    }

    @Test
    @DisplayName("save - Devrait persister un nouveau contact")
    void save_ShouldPersistNewContact() {
        // Arrange
        Contact contact = TestDataBuilder.createContact(null, "Emma Watson", "emma@example.com",
                ApplicationConstants.OPEN_MESSAGE);

        // Act
        Contact savedContact = contactRepository.save(contact);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Contact foundContact = contactRepository.findById(savedContact.getContactId()).orElseThrow();
        assertThat(foundContact.getContactId()).isNotNull();
        assertThat(foundContact.getName()).isEqualTo("Emma Watson");
        assertThat(foundContact.getEmail()).isEqualTo("emma@example.com");
        assertThat(foundContact.getStatus()).isEqualTo(ApplicationConstants.OPEN_MESSAGE);
        assertThat(foundContact.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("save - Devrait mettre à jour un contact existant")
    void save_ShouldUpdateExistingContact() {
        // Arrange
        Contact contact = createAndPersistContact("Robert Martin", "robert@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Long contactId = contact.getContactId();

        // Modifier le statut
        contact.setStatus(ApplicationConstants.CLOSED_MESSAGE);

        // Act
        contactRepository.save(contact);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Contact updatedContact = contactRepository.findById(contactId).orElseThrow();
        assertThat(updatedContact.getStatus()).isEqualTo(ApplicationConstants.CLOSED_MESSAGE);
    }

    @Test
    @DisplayName("findById - Devrait retourner le contact par ID")
    void findById_ShouldReturnContact_WhenExists() {
        // Arrange
        Contact contact = createAndPersistContact("Lisa Chen", "lisa@example.com",
                ApplicationConstants.OPEN_MESSAGE);

        // Act
        Contact foundContact = contactRepository.findById(contact.getContactId()).orElse(null);

        // Assert
        assertThat(foundContact).isNotNull();
        assertThat(foundContact.getName()).isEqualTo("Lisa Chen");
        assertThat(foundContact.getEmail()).isEqualTo("lisa@example.com");
    }

    @Test
    @DisplayName("findById - Devrait retourner Optional.empty si contact n'existe pas")
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // Act
        var result = contactRepository.findById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteById - Devrait supprimer le contact")
    void deleteById_ShouldDeleteContact() {
        // Arrange
        Contact contact = createAndPersistContact("Michael Scott", "michael@example.com",
                ApplicationConstants.OPEN_MESSAGE);
        Long contactId = contact.getContactId();

        // Act
        contactRepository.deleteById(contactId);
        entityManager.flush();

        // Assert
        assertThat(contactRepository.findById(contactId)).isEmpty();
    }

    @Test
    @DisplayName("count - Devrait retourner le nombre total de contacts")
    void count_ShouldReturnTotalNumberOfContacts() {
        // Arrange
        createAndPersistContact("User 1", "user1@example.com", ApplicationConstants.OPEN_MESSAGE);
        createAndPersistContact("User 2", "user2@example.com", ApplicationConstants.OPEN_MESSAGE);
        createAndPersistContact("User 3", "user3@example.com", ApplicationConstants.CLOSED_MESSAGE);

        // Act
        long count = contactRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("findAll - Devrait retourner tous les contacts")
    void findAll_ShouldReturnAllContacts() {
        // Arrange
        createAndPersistContact("Contact A", "a@example.com", ApplicationConstants.OPEN_MESSAGE);
        createAndPersistContact("Contact B", "b@example.com", ApplicationConstants.CLOSED_MESSAGE);

        // Act
        List<Contact> allContacts = contactRepository.findAll();

        // Assert
        assertThat(allContacts).hasSize(2);
    }

    // ==================== HELPER METHODS ====================

    private Contact createAndPersistContact(String name, String email, String status) {
        Contact contact = new Contact();
        contact.setName(name);
        contact.setEmail(email);
        contact.setMobileNumber("0612345678");
        contact.setMessage("Test message from " + name);
        contact.setStatus(status);
        contact.setCreatedAt(Instant.now());
        contact.setCreatedBy("system");

        Contact savedContact = entityManager.persist(contact);
        entityManager.flush();
        return savedContact;
    }
}

 */