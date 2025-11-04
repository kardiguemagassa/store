package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.dto.ProfileRequestDto;
import com.store.store.entity.Address;
import com.store.store.entity.Customer;
import com.store.store.repository.CustomerRepository;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Slf4j
@DisplayName("Tests d'Intégration - ProfileController")
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Nettoyer les données
        customerRepository.deleteAll();

        // Créer un client de test
        testCustomer = TestDataBuilder.createCustomer(null, "John", "Doe", "john.doe@example.com");
        testCustomer = customerRepository.save(testCustomer);

        log.info("✅ Configuration test terminée - Customer: {}", testCustomer.getEmail());
    }

    // ==================== TESTS GET /api/v1/profile ====================

    @Test
    @DisplayName("GET /api/v1/profile - Devrait retourner le profil du client")
    @WithMockUser(username = "john.doe@example.com")
    void getProfile_ShouldReturnProfile() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(testCustomer.getCustomerId()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.mobileNumber").exists())
                .andExpect(jsonPath("$.emailUpdated").value(false));

        log.info("✅ Profil récupéré avec succès");
    }

    @Test
    @DisplayName("GET /api/v1/profile - Devrait retourner le profil avec adresse")
    @WithMockUser(username = "john.doe@example.com")
    void getProfile_WithAddress_ShouldReturnProfileWithAddress() throws Exception {
        // Given - Ajouter une adresse au customer
        Address address = TestDataBuilder.createAddress(testCustomer);
        testCustomer.setAddress(address);
        customerRepository.save(testCustomer);

        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(testCustomer.getCustomerId()))
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.address.street").value("123 Main Street"))
                .andExpect(jsonPath("$.address.city").value("Paris"))
                .andExpect(jsonPath("$.address.postalCode").value("75001"));

        log.info("✅ Profil avec adresse récupéré avec succès");
    }

    @Test
    @DisplayName("GET /api/v1/profile - Devrait échouer sans authentification")
    void getProfile_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("✅ Sécurité: authentification requise vérifiée");
    }

    // ==================== TESTS PUT /api/v1/profile ====================

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait mettre à jour le profil")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithValidData_ShouldUpdateSuccessfully() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto(
                "John Updated",
                "john.doe@example.com",
                "0698765432",
                "456 Updated Street",
                "Lyon",
                "Auvergne-Rhône-Alpes",
                "69001",
                "FR"
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.mobileNumber").value("0698765432"))
                .andExpect(jsonPath("$.address.street").value("456 Updated Street"))
                .andExpect(jsonPath("$.address.city").value("Lyon"));

        // Vérifier en base
        Customer updatedCustomer = customerRepository.findByEmail("john.doe@example.com").orElseThrow();
        assert updatedCustomer.getName().equals("John Updated");

        log.info("✅ Profil mis à jour avec succès");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait échouer avec données invalides")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given - Nom vide
        ProfileRequestDto requestDto = new ProfileRequestDto();
        requestDto.setName("");  // Nom vide
        requestDto.setEmail("john.doe@example.com");
        requestDto.setMobileNumber("invalid");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        log.info("✅ Validation des données invalides vérifiée");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait mettre à jour seulement le profil sans adresse")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithoutAddress_ShouldUpdateOnlyCustomer() throws Exception {
        // Given - Mise à jour des champs de base SEULEMENT
        ProfileRequestDto requestDto = new ProfileRequestDto();
        requestDto.setName("John Updated");
        requestDto.setEmail("john.doe@example.com"); // Même email
        requestDto.setMobileNumber("0698765432");

        // ✅ CORRECTION : CHAMPS D'ADRESSE NULL - pas de chaînes vides
        requestDto.setStreet(null);
        requestDto.setCity(null);
        requestDto.setState(null);
        requestDto.setPostalCode(null);
        requestDto.setCountry(null);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.mobileNumber").value("0698765432"))
                .andExpect(jsonPath("$.address").doesNotExist()); // Pas d'adresse créée/mise à jour

        log.info("✅ Mise à jour du profil sans adresse réussie");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait échouer avec adresse partielle")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithPartialAddress_ShouldReturnError() throws Exception {
        // Given - Adresse partielle (rue seulement)
        ProfileRequestDto requestDto = new ProfileRequestDto();
        requestDto.setName("John Partial");
        requestDto.setEmail("john.doe@example.com");
        requestDto.setMobileNumber("0612345678");
        requestDto.setStreet("123 Rue Partielle"); // Rue seulement
        requestDto.setCity(""); // Ville vide
        requestDto.setState("");
        requestDto.setPostalCode("");
        requestDto.setCountry("");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest()); // Ou gérer l'IllegalArgumentException

        log.info("✅ Adresse partielle correctement rejetée");
    }

    /*@Test
    @DisplayName("PUT /api/v1/profile - Devrait créer une adresse complète")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithCompleteAddress_ShouldCreateAddress() throws Exception {
        // Given - Adresse complète
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();

        // S'assurer que tous les champs sont non vides
        requestDto.setStreet("123 Rue Complete");
        requestDto.setCity("Lyon");
        requestDto.setState("Rhône");
        requestDto.setPostalCode("69001");
        requestDto.setCountry("FR");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.address.street").value("123 Rue Complete"))
                .andExpect(jsonPath("$.address.postalCode").value("69001"));

        log.info("✅ Adresse complète créée avec succès");
    }*/

    /*@Test
    @DisplayName("PUT /api/v1/profile - Devrait échouer sans authentification")
    void updateProfile_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("✅ Sécurité: authentification requise vérifiée");
    }*/

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait gérer le changement d'email")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithNewEmail_ShouldFlagEmailUpdated() throws Exception {
        // Given - Changement d'email
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto(
                "John Doe",
                "john.newemail@example.com",  // Nouvel email
                "0612345678",
                "123 Main Street",
                "Paris",
                "Ile-de-France",
                "75001",
                "FR"
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.newemail@example.com"))
                .andExpect(jsonPath("$.emailUpdated").value(true));

        log.info("✅ Changement d'email détecté correctement");
    }

    /*@Test
    @DisplayName("PUT /api/v1/profile - Devrait créer une nouvelle adresse")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithNewAddress_ShouldCreateAddress() throws Exception {
        // Given - TOUS les champs obligatoires fournis
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.address.street").value("456 Updated Street"));

        // ✅ Correction : Utiliser l'ID existant
        Customer updatedCustomer = customerRepository.findById(testCustomer.getCustomerId()).orElseThrow();
        assert updatedCustomer.getAddress() != null;

        log.info("✅ Nouvelle adresse créée avec succès");
    }*/

    /*@Test
    @DisplayName("PUT /api/v1/profile - Devrait mettre à jour une adresse existante")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithExistingAddress_ShouldUpdateAddress() throws Exception {
        // Given - Client avec adresse existante
        Address existingAddress = TestDataBuilder.createAddress(testCustomer,
                "Old Street", "Paris", "IDF", "75001", "FR");
        testCustomer.setAddress(existingAddress);
        customerRepository.save(testCustomer);

        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto(
                "John Doe",
                "john.doe@example.com",
                "0612345678",
                "New Street",
                "Lyon",
                "Rhone",
                "69001",
                "FR"
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.street").value("New Street"))
                .andExpect(jsonPath("$.address.city").value("Lyon"));

        // Vérifier que l'adresse a été mise à jour
        Customer updatedCustomer = customerRepository.findByEmail("john.doe@example.com").orElseThrow();
        assert updatedCustomer.getAddress().getStreet().equals("New Street");

        log.info("✅ Adresse existante mise à jour avec succès");
    }*/

    // ==================== TESTS DE VALIDATION ====================

    /*@Test
    @DisplayName("PUT /api/v1/profile - Devrait valider le format de l'email")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given - Email invalide
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        requestDto.setEmail("invalid-email");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        log.info("✅ Validation de l'email vérifiée");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait valider le numéro de téléphone")
    @WithMockUser(username = "john.doe@example.com")
    void updateProfile_WithInvalidPhone_ShouldReturnBadRequest() throws Exception {
        // Given - Téléphone invalide
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        requestDto.setMobileNumber("abc");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        log.info("✅ Validation du téléphone vérifiée");
    }*/

    // ==================== TESTS DE PERSISTANCE ====================

    @Test
    @DisplayName("Devrait vérifier la persistance des données")
    @WithMockUser(username = "john.doe@example.com")
    void shouldVerifyDataPersistence() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto(
                "Persistent User",
                "john.doe@example.com",
                "0699887766",
                "Persistent Street",
                "Marseille",
                "PACA",
                "13001",
                "FR"
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - Vérifier en base
        Customer persistedCustomer = customerRepository.findByEmail("john.doe@example.com").orElseThrow();
        assert persistedCustomer.getName().equals("Persistent User");
        assert persistedCustomer.getMobileNumber().equals("0699887766");

        Address persistedAddress = persistedCustomer.getAddress();
        assert persistedAddress != null;
        assert persistedAddress.getStreet().equals("Persistent Street");
        assert persistedAddress.getCity().equals("Marseille");

        log.info("✅ Persistance des données vérifiée avec succès");
    }

    // ==================== TESTS DE TYPES DE DONNÉES ====================

    @Test
    @DisplayName("Devrait retourner les bons types de données")
    @WithMockUser(username = "john.doe@example.com")
    void shouldReturnCorrectDataTypes() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").isNumber())
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.mobileNumber").isString())
                .andExpect(jsonPath("$.emailUpdated").isBoolean());

        log.info("✅ Types de données vérifiés avec succès");
    }
}