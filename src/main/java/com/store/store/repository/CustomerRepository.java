package com.store.store.repository;

import com.store.store.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);
    // Fetch join pour charger les rôles
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.roles WHERE c.email = :email")
    Optional<Customer> findByEmailWithRoles(@Param("email") String email);
    Optional<Customer> findByEmailOrMobileNumber(String email, String mobileNumber);
    boolean existsByEmailAndCustomerIdNot(String email, Long customerId);
}