package com.store.store.service;

import com.store.store.dto.RegisterRequestDto;
import com.store.store.entity.Role;

import java.util.Set;

public interface RoleAssignmentService {

    Set<Role> determineInitialRoles(RegisterRequestDto registerRequest);
    void promoteToAdmin(Long userId, String promotedByAdmin);
    void demoteFromAdmin(Long userId, String demotedByAdmin);

}
