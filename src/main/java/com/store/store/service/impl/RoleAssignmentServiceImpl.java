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

/**
 * An implementation of {@link IRoleAssignmentService} that provides services
 * for managing roles assigned to customers in the system. Features include
 * assigning roles, promoting/demoting users, and retrieving customers with their roles.
 *
 * This service ensures the integrity of role assignments through hierarchical
 * validations and avoids unauthorized assignments or removals.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RoleAssignmentServiceImpl implements IRoleAssignmentService {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final ExceptionFactory exceptionFactory;

    /**
     * Determines the initial roles for a user during the registration process.
     * The method assigns the default "ROLE_USER" to the new user and logs this assignment.
     *
     * @param registerRequest the user registration request containing user details such as email
     * @return a set of roles assigned to the user, with "ROLE_USER" included
     */
    @Override
    public Set<Role> determineInitialRoles(RegisterRequestDto registerRequest) {
        Set<Role> roles = new HashSet<>();

        Role userRole = getRole(RoleType.ROLE_USER);
        roles.add(userRole);

        log.info("Rôle USER assigné à: {}", registerRequest.getEmail());
        return roles;
    }

    /**
     * Promotes a customer to the role of ADMIN.
     * This method updates the customer's role to include ADMIN privileges, saving the change in the repository.
     * It logs the promotion action, warns if the user is already an ADMIN, and throws exceptions in case of validation errors.
     *
     * @param userId the ID of the customer to promote to ADMIN
     * @param promotedByAdmin the identifier (e.g., email or name) of the admin performing the promotion
     * @throws ResourceNotFoundException if no customer is found with the given ID
     * @throws BusinessException if the customer is already an ADMIN
     */
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

    /**
     * Demotes a customer by removing the ADMIN role from their assigned roles.
     * This method ensures that the customer is updated in the repository accordingly and logs the action.
     *
     * @param userId the ID of the customer whose ADMIN privileges are to be removed
     * @param demotedByAdmin the identifier (e.g., email or name) of the admin performing the demotion
     * @throws ResourceNotFoundException if no customer is found with the given ID
     */
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

    /**
     * Assigns a specific role to a customer based on their ID and the requested role type.
     * This method performs several validations, such as prohibiting the manual assignment of
     * the ROLE_USER, checking for duplicate roles, and optionally enforcing role hierarchy.
     *
     * @param customerId the unique identifier of the customer to whom the role will be assigned
     * @param roleType the type of role to assign to the customer
     * @throws ResourceNotFoundException if no customer is found with the given ID
     * @throws BusinessException if the role is already assigned to the customer or ROLE_USER is manually assigned
     */
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

    // Protection du ROLE_USER

    /**
     * Removes a specific role from a customer's assigned roles.
     * This method ensures that the role being removed is not the mandatory "ROLE_USER" role,
     * loads the customer from the repository, removes the specified role from the customer's assigned roles,
     * and saves the updated customer back to the repository. An exception is thrown
     * if the mandatory role is attempted to be removed or if the customer does not exist.
     *
     * @param customerId the unique identifier of the customer from whom the role will be removed
     * @param roleType the type of role to remove from the customer's assigned roles
     * @throws BusinessException if an attempt is made to remove the mandatory "ROLE_USER" role
     * @throws ResourceNotFoundException if no customer is found with the given ID
     */
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

    /**
     * Retrieves a paginated list of customers along with their assigned roles.
     *
     * @param pageable the pagination and sorting information for retrieving the customers
     * @return a page of CustomerWithRolesDto containing customer details and their roles
     */
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
        return roleRepository.findByName(roleType).orElseThrow(() -> exceptionFactory.missingRole(roleType.name()));
    }

    /**
     * Validates the feasibility of changing a customer's role based on a predefined role hierarchy.
     * Ensures that customers transitioning to a new role meet specific prerequisite role conditions.
     * Throws a BusinessException if the validation fails.
     *
     * @param customer the customer whose role hierarchy is being validated
     * @param newRole the new role that the customer is intended to acquire
     * @throws BusinessException if the customer does not fulfill the prerequisite role conditions
     *         for the specified new role
     */
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

    /**
     * Checks whether the given customer is already assigned the role of "ROLE_ADMIN".
     *
     * @param customer the customer whose roles are to be checked
     * @return {@code true} if the customer has the "ROLE_ADMIN" role, {@code false} otherwise
     */
    private boolean isAlreadyAdmin(Customer customer) {
        return customer.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }
}