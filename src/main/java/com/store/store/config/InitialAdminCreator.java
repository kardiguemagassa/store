package com.store.store.config;

import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class InitialAdminCreator implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Vérifier si un admin existe déjà
        if (customerRepository.findByEmail("admin@store.com").isEmpty()) {
            Customer admin = new Customer();
            admin.setName("Admin Principal");
            admin.setEmail("admin@store.com");
            admin.setMobileNumber("+33600000000");
            admin.setPasswordHash(passwordEncoder.encode("AdminSecurePass123!"));

            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.findByName(RoleType.ROLE_USER).orElseThrow());
            roles.add(roleRepository.findByName(RoleType.ROLE_ADMIN).orElseThrow());
            admin.setRoles(roles);

            customerRepository.save(admin);
            log.info("✅ Premier administrateur créé : admin@store.com");
        }
    }
}
