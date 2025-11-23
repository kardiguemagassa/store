package com.store.store.service.impl;

import com.store.store.dto.user.CustomerWithRolesDto;
import com.store.store.dto.auth.RegisterRequestDto;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-06
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RoleAssignmentServiceImpl implements IRoleAssignmentService {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;
    private final CacheManager cacheManager;


    // CONSULTATION DES UTILISATEURS - AVEC CACHE INVALIDÉ
    @Override
    public Page<CustomerWithRolesDto> getAllCustomersWithRoles(Pageable pageable) {
        // S'ASSURER QUE LE CACHE EST FRAIS EN INVALIDANT AVANT LECTURE
        evictRolesCache();

        Page<Customer> customers = customerRepository.findAll(pageable);

        return customers.map(customer -> CustomerWithRolesDto.builder()
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .email(customer.getEmail())
                .roles(customer.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()))
                .createdAt(customer.getCreatedAt())
                .build());
    }

    @Override
    public Set<Role> determineInitialRoles(RegisterRequestDto registerRequest) {
        Set<Role> roles = new HashSet<>();

        Role userRole = getRole(RoleType.ROLE_USER);
        roles.add(userRole);

        log.info("ROLE_USER assigned to new user: {}", registerRequest.getEmail());
        return roles;
    }

    // ATTRIBUTION MANUELLE DE RÔLE - AVEC INVALIDATION CACHE
    @Override
    public void assignRole(Long customerId, RoleType roleType) {
        // Récupération de l'utilisateur
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("User", "id", customerId.toString()));

        // Validation 1 : Interdire l'attribution manuelle de ROLE_USER
        if (roleType == RoleType.ROLE_USER) {
            throw exceptionFactory.businessError(messageService.getMessage("error.role.user.auto.assigned"));
        }

        // Validation 2 : Vérifier que le rôle n'existe pas déjà
        if (customer.hasRole(roleType)) {
            throw exceptionFactory.businessError(messageService.getMessage("error.role.already.assigned", roleType.getDisplayName()));
        }

        // Validation 3 : Respecter la hiérarchie des rôles
        validateRoleHierarchy(customer, roleType);

        // Attribution du rôle
        Role role = getRole(roleType);
        customer.getRoles().add(role);
        customerRepository.save(customer);

        // INVALIDATION EXPLICITE DU CACHE ROLES
        evictRolesCache();

        log.info("Role {} assigned to customer {} ({})", roleType, customerId, customer.getEmail());
    }

    // PROMOTION VERS ADMIN
    @Override
    public void promoteToAdmin(Long userId, String promotedByAdmin) {
        // Récupération de l'utilisateur
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("User", "id", userId.toString()));

        // Vérification : pas déjà ADMIN
        if (customer.isAdmin()) {
            log.warn("User {} is already ADMIN", customer.getEmail());
            //  Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.role.user.already.admin"));
        }

        // Attribution du rôle ADMIN
        Role adminRole = getRole(RoleType.ROLE_ADMIN);
        customer.getRoles().add(adminRole);

        customerRepository.save(customer);
        log.info("User {} promoted to ADMIN by {}", customer.getEmail(), promotedByAdmin);
    }

    // RÉTROGRADATION DEPUIS ADMIN
    @Override
    public void demoteFromAdmin(Long userId, String demotedByAdmin) {
        // Récupération de l'utilisateur
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("User", "id", userId.toString()));

        // Retrait du rôle ADMIN
        Role adminRole = getRole(RoleType.ROLE_ADMIN);
        customer.getRoles().remove(adminRole);

        customerRepository.save(customer);
        log.info("ADMIN privileges removed from user {} by {}", customer.getEmail(), demotedByAdmin);
    }

    // RETRAIT DE RÔLE - VERSION AVEC INVALIDATION CACHE
    @Override
    public void removeRole(Long customerId, RoleType roleType) {
        // Protection du ROLE_USER obligatoire
        if (roleType == RoleType.ROLE_USER) {
            throw exceptionFactory.businessError(messageService.getMessage("error.role.user.cannot.remove"));
        }

        // Récupération de l'utilisateur
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("User", "id", customerId.toString()));

        // Vérification explicite que le rôle existe
        boolean roleRemoved = customer.getRoles().removeIf(role -> role.getName().equals(roleType));

        if (!roleRemoved) {
            throw exceptionFactory.businessError(messageService.getMessage("error.role.not.assigned", roleType.getDisplayName()));
        }

        customerRepository.save(customer);

        // INVALIDATION EXPLICITE DU CACHE ROLES
        evictRolesCache();

        log.info("Role {} successfully removed from customer {}", roleType, customerId);
    }

    // MÉTHODE D'INVALIDATION DU CACHE ROLES
    private void evictRolesCache() {
        try {
            Cache rolesCache = cacheManager.getCache("roles");
            if (rolesCache != null) {
                rolesCache.clear();
                log.debug("Roles cache evicted successfully");
            }
        } catch (Exception e) {
            log.warn(" Could not evict roles cache: {}", e.getMessage());
        }
    }

    // MÉTHODES PRIVÉES - UTILITAIRES
    private Role getRole(RoleType roleType) {
        return roleRepository.findByName(roleType).orElseThrow(() -> exceptionFactory.missingRole(roleType.name()));
    }

    // VALIDATION - HIÉRARCHIE DES RÔLES
    private void validateRoleHierarchy(Customer customer, RoleType newRole) {
        switch (newRole) {
            case ROLE_EMPLOYEE:
                // Un USER peut devenir EMPLOYEE
                if (!customer.hasRole(RoleType.ROLE_USER)) {
                    throw exceptionFactory.businessError(messageService.getMessage("error.role.hierarchy.user.required"));
                }
                break;

            case ROLE_MANAGER:
                // Un EMPLOYEE peut devenir MANAGER
                if (!customer.isEmployee() && !customer.isManager()) {
                    // Utilisation de messageService
                    throw exceptionFactory.businessError(messageService.getMessage("error.role.hierarchy.employee.required"));
                }
                break;

            case ROLE_ADMIN:
                // Seuls les MANAGER peuvent devenir ADMIN (règle stricte)
                if (!customer.isManager() && !customer.isAdmin()) {
                    // Utilisation de messageService
                    throw exceptionFactory.businessError(messageService.getMessage("error.role.hierarchy.manager.required"));
                }
                break;
        }
    }

}