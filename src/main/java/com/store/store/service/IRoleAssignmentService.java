package com.store.store.service;

import com.store.store.dto.CustomerWithRolesDto;
import com.store.store.dto.RegisterRequestDto;
import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface IRoleAssignmentService {

    Set<Role> determineInitialRoles(RegisterRequestDto registerRequest);
    void promoteToAdmin(Long userId, String promotedByAdmin);
    void demoteFromAdmin(Long userId, String demotedByAdmin);

    // ✅ AJOUTER : Nouvelles méthodes pour gestion complète des rôles
    void assignRole(Long customerId, RoleType roleType);
    void removeRole(Long customerId, RoleType roleType);
    Page<CustomerWithRolesDto> getAllCustomersWithRoles(Pageable pageable);

}
