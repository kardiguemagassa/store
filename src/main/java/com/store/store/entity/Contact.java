package com.store.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "contacts")
@NamedQuery(name = "Contact.findByStatus",
        query = "SELECT c FROM Contact c WHERE c.status = :status")
@NamedNativeQuery(name = "Contact.findByStatusWithNativeQuery",
        query = "SELECT * FROM contacts WHERE status = :status",
        resultClass = Contact.class)
public class Contact extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id", nullable = false)
    private Long contactId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Size(min = 7, max = 20, message = "Le numéro de téléphone doit contenir entre 7 et 20 caractères")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Format de numéro de téléphone invalide")
    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "status", nullable = false, length = 50)
    private String status;


}
