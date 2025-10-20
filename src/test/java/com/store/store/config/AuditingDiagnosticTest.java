package com.store.store.config;

import com.store.store.entity.Role;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
class AuditingDiagnosticTest {

    @Autowired(required = false)
    private AuditorAware<String> auditorAware;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void diagnostic_AuditorAwareBean_ShouldExist() {
        log.info("=== DIAGNOSTIC 1 : AuditorAware Bean ===");
        assertNotNull(auditorAware, "AuditorAware bean n'existe pas !");
        log.info("AuditorAware bean existe");

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertTrue(auditor.isPresent(), "getCurrentAuditor() retourne empty !");
        log.info("Current auditor : " + auditor.get());
    }

    @Test
    void diagnostic_BaseEntity_ShouldAutoFillAuditFields() {
        log.info("\n=== DIAGNOSTIC 2 : Auto-fill des champs audit ===");

        // Role qui étend BaseEntity
        Role role = new Role();
        role.setName("TEST_ROLE");
        role.setCreatedBy("MANUAL");  // manuellement

        Role saved = roleRepository.save(role);

        log.info("createdAt : " + saved.getCreatedAt());
        log.info("createdBy : " + saved.getCreatedBy());
        log.info("updatedAt : " + saved.getUpdatedAt());
        log.info("updatedBy : " + saved.getUpdatedBy());

        assertNotNull(saved.getCreatedAt(), "createdAt est null !");
        log.info("createdAt est rempli");

        // Modification
        saved.setName("MODIFIED_ROLE");
        roleRepository.save(saved);

        Role updated = roleRepository.findById(saved.getRoleId()).orElseThrow();
        log.info("Après modification :");
        log.info("updatedAt : " + updated.getUpdatedAt());
        log.info("updatedBy : " + updated.getUpdatedBy());

        assertNotNull(updated.getUpdatedAt(), "updatedAt est null après modification !");
        log.info("updatedAt est rempli après modification");
    }
}