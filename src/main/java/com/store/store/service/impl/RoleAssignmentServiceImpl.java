package com.store.store.service.impl;

import com.store.store.dto.RegisterRequestDto;
import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.exception.ExceptionFactory;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import com.store.store.service.RoleAssignmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RoleAssignmentServiceImpl implements RoleAssignmentService {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final ExceptionFactory exceptionFactory;

    @Override
    public Set<Role> determineInitialRoles(RegisterRequestDto registerRequest) {
        Set<Role> roles = new HashSet<>();

        Role userRole = getRole("ROLE_USER");
        roles.add(userRole);

        log.info("Rôle USER assigné à: {}", registerRequest.getEmail());
        return roles;
    }

    @Override
    public void promoteToAdmin(Long userId, String promotedByAdmin) {
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("Utilisateur", "id", userId.toString()));

        // Vérifier si l'utilisateur n'est pas déjà admin
        if (isAlreadyAdmin(customer)) {
            log.warn("L'utilisateur {} est déjà ADMIN", customer.getEmail());
            throw exceptionFactory.businessError("L'utilisateur est déjà administrateur");
        }

        Role adminRole = getRole("ROLE_ADMIN");
        customer.getRoles().add(adminRole);

        customerRepository.save(customer);

        log.info("Utilisateur {} promu ADMIN par {}", customer.getEmail(), promotedByAdmin);
    }

    public void demoteFromAdmin(Long userId, String demotedByAdmin) {
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("Utilisateur", "id", userId.toString()));

        // Ne pas permettre de se retirer soi-même les droits admin
        Role adminRole = getRole("ROLE_ADMIN");
        customer.getRoles().remove(adminRole);

        customerRepository.save(customer);

        log.info("Privilèges ADMIN retirés pour {} par {}", customer.getEmail(), demotedByAdmin);
    }

    private boolean isAlreadyAdmin(Customer customer) {
        return customer.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    private Role getRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> exceptionFactory.missingRole(roleName));
    }
}