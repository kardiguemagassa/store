package com.store.store.entity;

import com.store.store.enums.RoleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class testRoleHierarchy {

    @Test
    public void testRoleHierarchy() {
        // Vérifier la hiérarchie des niveaux
        assertTrue(RoleType.ROLE_ADMIN.isHigherThan(RoleType.ROLE_MANAGER));
        assertTrue(RoleType.ROLE_MANAGER.isHigherThan(RoleType.ROLE_EMPLOYEE));
        assertTrue(RoleType.ROLE_EMPLOYEE.isHigherThan(RoleType.ROLE_USER));

        // Vérifier les méthodes utilitaires
        assertTrue(RoleType.ROLE_ADMIN.isAdmin());
        assertTrue(RoleType.ROLE_USER.isUser());
        assertTrue(RoleType.ROLE_MANAGER.isManager());
        assertTrue(RoleType.ROLE_EMPLOYEE.isEmployee());
    }
}
