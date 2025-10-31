package com.store.store.service.impl;

import com.store.store.dto.CustomerWithRolesDto;
import com.store.store.dto.RegisterRequestDto;
import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.exception.ResourceNotFoundException;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import com.store.store.service.IRoleAssignmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RoleAssignmentServiceImpl implements IRoleAssignmentService {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final ExceptionFactory exceptionFactory;

    @Override
    public Set<Role> determineInitialRoles(RegisterRequestDto registerRequest) {
        Set<Role> roles = new HashSet<>();

        Role userRole = getRole(RoleType.ROLE_USER);
        roles.add(userRole);

        log.info("Rôle USER assigné à: {}", registerRequest.getEmail());
        return roles;
    }

    @Override
    public void promoteToAdmin(Long userId, String promotedByAdmin) {
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "Utilisateur", "id", userId.toString()));

        if (customer.isAdmin()) {
            log.warn("L'utilisateur {} est déjà ADMIN", customer.getEmail());
            throw exceptionFactory.businessError("L'utilisateur est déjà administrateur");
        }

        Role adminRole = getRole(RoleType.ROLE_ADMIN);
        customer.getRoles().add(adminRole);

        customerRepository.save(customer);
        log.info("Utilisateur {} promu ADMIN par {}", customer.getEmail(), promotedByAdmin);
    }

    @Override
    public void demoteFromAdmin(Long userId, String demotedByAdmin) {
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "Utilisateur", "id", userId.toString()));

        Role adminRole = getRole(RoleType.ROLE_ADMIN);
        customer.getRoles().remove(adminRole);

        customerRepository.save(customer);
        log.info("Privilèges ADMIN retirés pour {} par {}", customer.getEmail(), demotedByAdmin);
    }

    // Assigner un rôle avec validation hiérarchique
    @Override
    public void assignRole(Long customerId, RoleType roleType) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Validation 1 : Interdire l'auto-attribution de ROLE_USER
        if (roleType == RoleType.ROLE_USER) {
            throw new BusinessException("ROLE_USER is assigned automatically at registration");
        }

        // Validation 2 : Vérifier si le rôle existe déjà
        if (customer.hasRole(roleType)) {
            throw new BusinessException("Customer already has role: " + roleType.getDisplayName());
        }

        // Validation 3 : Respecter la hiérarchie (optionnel)
        validateRoleHierarchy(customer, roleType);

        Role role = getRole(roleType);
        customer.getRoles().add(role);
        customerRepository.save(customer);

        log.info("Role {} assigned to customer {} ({})",
                roleType, customerId, customer.getEmail());
    }

    // ✅ EXISTANT : Protection du ROLE_USER
    @Override
    public void removeRole(Long customerId, RoleType roleType) {
        if (roleType == RoleType.ROLE_USER) {
            throw new BusinessException("Cannot remove ROLE_USER - it's required for all users");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Role role = getRole(roleType);
        customer.getRoles().remove(role);
        customerRepository.save(customer);

        log.info("Role {} removed from customer {}", roleType, customerId);
    }

    @Override
    public Page<CustomerWithRolesDto> getAllCustomersWithRoles(Pageable pageable) {
        Page<Customer> customers = customerRepository.findAll(pageable);

        return customers.map(customer -> CustomerWithRolesDto.builder()
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .email(customer.getEmail())
                .roles(customer.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .build());
    }

    private Role getRole(RoleType roleType) {
        return roleRepository.findByName(roleType)
                .orElseThrow(() -> exceptionFactory.missingRole(roleType.name()));
    }

    // Validation hiérarchique optionnelle mais recommandée
    private void validateRoleHierarchy(Customer customer, RoleType newRole) {
        switch (newRole) {
            case ROLE_EMPLOYEE:
                // Un USER peut devenir EMPLOYEE
                if (!customer.hasRole(RoleType.ROLE_USER)) {
                    throw new BusinessException("User must have ROLE_USER first");
                }
                break;

            case ROLE_MANAGER:
                // Un EMPLOYEE peut devenir MANAGER
                if (!customer.isEmployee() && !customer.isManager()) {
                    throw new BusinessException("User should be EMPLOYEE before becoming MANAGER");
                }
                break;

            case ROLE_ADMIN:
                // Seuls les MANAGER peuvent devenir ADMIN (règle stricte)
                if (!customer.isManager() && !customer.isAdmin()) {
                    throw new BusinessException("User must be MANAGER before becoming ADMIN");
                }
                break;
        }
    }

    private boolean isAlreadyAdmin(Customer customer) {
        return customer.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }
}