package com.store.store.service.impl;

import com.store.store.dto.address.AddressDto;
import com.store.store.dto.profile.ProfileRequestDto;
import com.store.store.dto.profile.ProfileResponseDto;
import com.store.store.entity.Address;
import com.store.store.entity.Customer;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.repository.CustomerRepository;
import com.store.store.service.IProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements IProfileService {

    private final CustomerRepository customerRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;

    // RÉCUPÉRATION DU PROFIL

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile() {
        try {
            log.info("Fetching profile for authenticated customer");

            // Récupération du Customer authentifié
            Customer customer = getAuthenticatedCustomer();

            // Mapping vers DTO
            ProfileResponseDto profile = mapCustomerToProfileResponseDto(customer);

            log.info("Profile retrieved successfully for customer: {}", customer.getEmail());
            return profile;

        } catch (UsernameNotFoundException e) {
            log.warn("Customer not found while fetching profile");
            // Utilisation de messageService
            throw exceptionFactory.businessErrorWithCode(messageService.getMessage("error.profile.customer.not.found"));

        } catch (DataAccessException e) {
            log.error("Database error while fetching profile", e);
            // Utilisation de messageService
            throw exceptionFactory.businessErrorWithCode(messageService.getMessage("error.profile.fetch.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while fetching profile", e);
            // Utilisation de messageService
            throw exceptionFactory.businessErrorWithCode(messageService.getMessage("error.unexpected.profile.fetch"));
        }
    }


    // MISE À JOUR DU PROFILE

    @Override
    @Transactional
    public ProfileResponseDto updateProfile(ProfileRequestDto profileRequestDto) {
        try {
            log.info("Updating profile for authenticated customer");

            // 1. Récupération de l'utilisateur authentifié
            Customer customer = getAuthenticatedCustomer();

            // 2. Détection changement d'email
            boolean isEmailUpdated = !customer.getEmail().equals(profileRequestDto.getEmail().trim());

            // 3. Validation unicité de l'email (si modifié)
            if (isEmailUpdated && customerRepository.existsByEmailAndCustomerIdNot(
                    profileRequestDto.getEmail(), customer.getCustomerId())) {
                // Utilisation de messageService
                throw exceptionFactory.businessErrorWithCode(messageService.getMessage("error.profile.email.already.exists", profileRequestDto.getEmail()));
            }

            // 4. Mise à jour des informations de base
            updateCustomerFromRequest(customer, profileRequestDto);

            // 5. Gestion de l'adresse
            updateCustomerAddress(customer, profileRequestDto);

            // 6. Sauvegarde
            Customer savedCustomer = customerRepository.save(customer);

            // 7. Construction de la réponse
            ProfileResponseDto profileResponse = mapCustomerToProfileResponseDto(savedCustomer);
            profileResponse.setEmailUpdated(isEmailUpdated);

            log.info("Profile updated successfully for customer: {}", savedCustomer.getEmail());
            return profileResponse;

        } catch (BusinessException e) {
            throw e; // Relance les exceptions métier sans modification

        } catch (DataAccessException e) {
            log.error("Database error while updating profile", e);
            // Utilisation de messageService
            throw exceptionFactory.businessErrorWithCode(messageService.getMessage("error.profile.update.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while updating profile", e);
            // Utilisation de messageService
            throw exceptionFactory.businessErrorWithCode(messageService.getMessage("error.unexpected.profile.update"));
        }
    }

    // RÉCUPÉRATION DU CUSTOMER AUTHENTIFIÉ

    @Override
    @Transactional(readOnly = true)
    public Customer getAuthenticatedCustomer() {
        try {
            // Récupération de l'authentification
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            // Recherche du Customer
            return customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("Authenticated customer not found in database: {}", email);
                        return exceptionFactory.businessErrorWithCode(messageService.getMessage("error.profile.customer.not.found"));
                    });

        } catch (Exception e) {
            log.error("Error while getting authenticated customer", e);
            throw exceptionFactory.businessErrorWithCode(messageService.getMessage("error.profile.authentication.failed"));
        }
    }

    // MÉTHODES PRIVÉES - MISE À JOUR

    private void updateCustomerFromRequest(Customer customer, ProfileRequestDto profileRequestDto) {
        customer.setName(profileRequestDto.getName().trim());
        customer.setEmail(profileRequestDto.getEmail().trim());
        customer.setMobileNumber(profileRequestDto.getMobileNumber().trim());
    }

    private void updateCustomerAddress(Customer customer, ProfileRequestDto profileRequestDto) {
        if (hasCompleteAddressData(profileRequestDto)) {
            // Cas 1 : Adresse complète → Créer ou mettre à jour
            Address address = customer.getAddress();
            if (address == null) {
                address = new Address();
                address.setCustomer(customer); // Relation bidirectionnelle
            }

            // Mise à jour des champs (trimmed)
            address.setStreet(profileRequestDto.getStreet().trim());
            address.setCity(profileRequestDto.getCity().trim());
            address.setState(profileRequestDto.getState().trim());
            address.setPostalCode(profileRequestDto.getPostalCode().trim());
            address.setCountry(profileRequestDto.getCountry().trim());

            customer.setAddress(address);

        } else if (hasPartialAddressData(profileRequestDto)) {
            // Cas 2 : Adresse partielle → Supprimer (données incohérentes)
            customer.setAddress(null);
            log.debug("Partial address data provided, removing existing address");
        }
        // Cas 3 : Aucune donnée d'adresse → Pas de changement
    }

    // MÉTHODES PRIVÉES - VALIDATION

    private boolean hasCompleteAddressData(ProfileRequestDto dto) {
        return dto.getStreet() != null && !dto.getStreet().trim().isEmpty() &&
                dto.getCity() != null && !dto.getCity().trim().isEmpty() &&
                dto.getState() != null && !dto.getState().trim().isEmpty() &&
                dto.getPostalCode() != null && !dto.getPostalCode().trim().isEmpty() &&
                dto.getCountry() != null && !dto.getCountry().trim().isEmpty();
    }

    private boolean hasPartialAddressData(ProfileRequestDto dto) {
        boolean hasSomeData = (dto.getStreet() != null && !dto.getStreet().trim().isEmpty()) ||
                (dto.getCity() != null && !dto.getCity().trim().isEmpty()) ||
                (dto.getState() != null && !dto.getState().trim().isEmpty()) ||
                (dto.getPostalCode() != null && !dto.getPostalCode().trim().isEmpty()) ||
                (dto.getCountry() != null && !dto.getCountry().trim().isEmpty());

        boolean hasAllData = hasCompleteAddressData(dto);

        return hasSomeData && !hasAllData;
    }

    // MÉTHODES PRIVÉES - MAPPING

    private ProfileResponseDto mapCustomerToProfileResponseDto(Customer customer) {
        ProfileResponseDto profileResponseDto = new ProfileResponseDto();
        profileResponseDto.setCustomerId(customer.getCustomerId());
        profileResponseDto.setName(customer.getName());
        profileResponseDto.setEmail(customer.getEmail());
        profileResponseDto.setMobileNumber(customer.getMobileNumber());
        profileResponseDto.setEmailUpdated(false); // Par défaut, pas de modification

        // Mapping de l'adresse si elle existe
        if (customer.getAddress() != null) {
            AddressDto addressDto = new AddressDto();
            addressDto.setStreet(customer.getAddress().getStreet());
            addressDto.setCity(customer.getAddress().getCity());
            addressDto.setState(customer.getAddress().getState());
            addressDto.setPostalCode(customer.getAddress().getPostalCode());
            addressDto.setCountry(customer.getAddress().getCountry());
            profileResponseDto.setAddress(addressDto);
        }

        return profileResponseDto;
    }

}