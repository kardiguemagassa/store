package com.store.store.service;

import com.store.store.dto.user.CustomerWithRolesDto;
import com.store.store.dto.auth.RegisterRequestDto;
import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
public interface IRoleAssignmentService {

    Set<Role> determineInitialRoles(RegisterRequestDto registerRequest);
    void promoteToAdmin(Long userId, String promotedByAdmin);
    void demoteFromAdmin(Long userId, String demotedByAdmin);

    // gestion des rôles
    void assignRole(Long customerId, RoleType roleType);
    void removeRole(Long customerId, RoleType roleType);
    Page<CustomerWithRolesDto> getAllCustomersWithRoles(Pageable pageable);

}
