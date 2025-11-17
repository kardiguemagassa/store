package com.store.store.service.impl;

import com.store.store.dto.auth.RegisterRequestDto;
import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ConfigurationException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ExceptionFactory exceptionFactory;

    @InjectMocks
    private RoleAssignmentServiceImpl roleAssignmentService;

    private Customer testCustomer;
    private Role userRole;
    private Role adminRole;
    private RegisterRequestDto registerRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        userRole = new Role();
        userRole.setName(RoleType.ROLE_USER);
        userRole.setRoleId(1L);

        adminRole = new Role();
        adminRole.setName(RoleType.ROLE_ADMIN);
        adminRole.setRoleId(2L);

        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);
        testCustomer.setEmail("test@example.com");
        testCustomer.setRoles(new HashSet<>(Set.of(userRole)));

        registerRequest = new RegisterRequestDto();
        registerRequest.setEmail("test@example.com");
        registerRequest.setName("Test User");
        registerRequest.setMobileNumber("0612345678");
        registerRequest.setPassword("password123");
    }

    @Test
    @DisplayName("Devrait retourner uniquement le rôle USER pour une inscription normale")
    void determineInitialRoles_ShouldReturnOnlyUserRole() {
        // Given
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(userRole));

        // When
        Set<Role> roles = roleAssignmentService.determineInitialRoles(registerRequest);

        // Then
        assertThat(roles).hasSize(1);
        assertThat(roles).containsOnly(userRole);
        verify(roleRepository, times(1)).findByName(RoleType.ROLE_USER);
        verify(roleRepository, never()).findByName(RoleType.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Devrait lancer une exception si le rôle USER n'existe pas")
    void determineInitialRoles_ShouldThrowExceptionWhenUserRoleNotFound() {
        // Given
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.empty());
        when(exceptionFactory.missingRole("ROLE_USER"))
                .thenThrow(new ConfigurationException("Rôle ROLE_USER non trouvé"));

        // When & Then
        assertThatThrownBy(() -> roleAssignmentService.determineInitialRoles(registerRequest))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("ROLE_USER");

        verify(roleRepository, times(1)).findByName(RoleType.ROLE_USER);
    }

    @Test
    @DisplayName("Devrait promouvoir un utilisateur au rôle ADMIN")
    void promoteToAdmin_ShouldAddAdminRole() {
        // Given
        Long userId = 1L;
        String promotedBy = "admin@example.com";

        when(customerRepository.findById(userId)).thenReturn(Optional.of(testCustomer));
        when(roleRepository.findByName(RoleType.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // When
        roleAssignmentService.promoteToAdmin(userId, promotedBy);

        // Then
        assertThat(testCustomer.getRoles()).contains(adminRole, userRole);
        verify(customerRepository, times(1)).findById(userId);
        verify(roleRepository, times(1)).findByName(RoleType.ROLE_ADMIN);
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    @DisplayName("Devrait lancer une exception si l'utilisateur à promouvoir n'existe pas")
    void promoteToAdmin_ShouldThrowExceptionWhenUserNotFound() {
        // Given
        Long userId = 999L;
        String promotedBy = "admin@example.com";

        when(customerRepository.findById(userId)).thenReturn(Optional.empty());
        when(exceptionFactory.resourceNotFound("Utilisateur", "id", "999"))
                .thenThrow(new BusinessException("Utilisateur non trouvé"));

        // When & Then
        assertThatThrownBy(() -> roleAssignmentService.promoteToAdmin(userId, promotedBy))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Utilisateur non trouvé");

        verify(customerRepository, times(1)).findById(userId);
        verify(roleRepository, never()).findByName(RoleType.ROLE_ADMIN);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait lancer une exception si l'utilisateur est déjà ADMIN")
    void promoteToAdmin_ShouldThrowExceptionWhenUserIsAlreadyAdmin() {
        // Given
        Long userId = 1L;
        String promotedBy = "admin@example.com";

        // Ajouter le rôle ADMIN à l'utilisateur
        testCustomer.getRoles().add(adminRole);

        when(customerRepository.findById(userId)).thenReturn(Optional.of(testCustomer));
        when(exceptionFactory.businessError("L'utilisateur est déjà administrateur"))
                .thenThrow(new BusinessException("L'utilisateur est déjà administrateur"));

        // When & Then
        assertThatThrownBy(() -> roleAssignmentService.promoteToAdmin(userId, promotedBy))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà administrateur");

        verify(customerRepository, times(1)).findById(userId);
        verify(roleRepository, never()).findByName(RoleType.ROLE_ADMIN);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait lancer une exception si le rôle ADMIN n'existe pas")
    void promoteToAdmin_ShouldThrowExceptionWhenAdminRoleNotFound() {
        // Given
        Long userId = 1L;
        String promotedBy = "admin@example.com";

        when(customerRepository.findById(userId)).thenReturn(Optional.of(testCustomer));
        when(roleRepository.findByName(RoleType.ROLE_ADMIN)).thenReturn(Optional.empty());
        when(exceptionFactory.missingRole("ROLE_ADMIN"))
                .thenThrow(new ConfigurationException("Rôle ROLE_ADMIN non trouvé"));

        // When & Then
        assertThatThrownBy(() -> roleAssignmentService.promoteToAdmin(userId, promotedBy))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("ROLE_ADMIN");

        verify(customerRepository, times(1)).findById(userId);
        verify(roleRepository, times(1)).findByName(RoleType.ROLE_ADMIN);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait retirer le rôle ADMIN d'un utilisateur")
    void demoteFromAdmin_ShouldRemoveAdminRole() {
        // Given
        Long userId = 1L;
        String demotedBy = "admin@example.com";

        // L'utilisateur a actuellement les rôles USER et ADMIN
        testCustomer.getRoles().add(adminRole);

        when(customerRepository.findById(userId)).thenReturn(Optional.of(testCustomer));
        when(roleRepository.findByName(RoleType.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // When
        roleAssignmentService.demoteFromAdmin(userId, demotedBy);

        // Then
        assertThat(testCustomer.getRoles()).containsOnly(userRole);
        assertThat(testCustomer.getRoles()).doesNotContain(adminRole);
        verify(customerRepository, times(1)).findById(userId);
        verify(roleRepository, times(1)).findByName(RoleType.ROLE_ADMIN);
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    @DisplayName("Ne devrait pas lancer d'erreur si l'utilisateur n'a pas le rôle ADMIN")
    void demoteFromAdmin_ShouldNotFailWhenUserIsNotAdmin() {
        // Given
        Long userId = 1L;
        String demotedBy = "admin@example.com";

        // L'utilisateur n'a que le rôle USER
        testCustomer.setRoles(new HashSet<>(Set.of(userRole)));

        when(customerRepository.findById(userId)).thenReturn(Optional.of(testCustomer));
        when(roleRepository.findByName(RoleType.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // When
        roleAssignmentService.demoteFromAdmin(userId, demotedBy);

        // Then - Devrait réussir même si l'utilisateur n'était pas admin
        assertThat(testCustomer.getRoles()).containsOnly(userRole);
        verify(customerRepository, times(1)).findById(userId);
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    @DisplayName("Devrait lancer une exception si l'utilisateur à rétrograder n'existe pas")
    void demoteFromAdmin_ShouldThrowExceptionWhenUserNotFound() {
        // Given
        Long userId = 999L;
        String demotedBy = "admin@example.com";

        when(customerRepository.findById(userId)).thenReturn(Optional.empty());
        when(exceptionFactory.resourceNotFound("Utilisateur", "id", "999"))
                .thenThrow(new BusinessException("Utilisateur non trouvé"));

        // When & Then
        assertThatThrownBy(() -> roleAssignmentService.demoteFromAdmin(userId, demotedBy))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Utilisateur non trouvé");

        verify(customerRepository, times(1)).findById(userId);
        verify(roleRepository, never()).findByName(RoleType.ROLE_ADMIN);
        verify(customerRepository, never()).save(any());
    }
}