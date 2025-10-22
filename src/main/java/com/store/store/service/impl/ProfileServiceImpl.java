package com.store.store.service.impl;

import com.store.store.dto.AddressDto;
import com.store.store.dto.ProfileRequestDto;
import com.store.store.dto.ProfileResponseDto;
import com.store.store.entity.Address;
import com.store.store.entity.Customer;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;

import com.store.store.repository.CustomerRepository;
import com.store.store.service.IProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements IProfileService {

    private final CustomerRepository customerRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile() {
        try {
            log.info("Fetching profile for authenticated customer");

            Customer customer = getAuthenticatedCustomer();
            ProfileResponseDto profile = mapCustomerToProfileResponseDto(customer);

            log.info("Profile retrieved successfully for customer: {}", customer.getEmail());
            return profile;

        } catch (UsernameNotFoundException e) {
            log.warn("Customer not found while fetching profile");
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.profile.customer.not.found")
            );

        } catch (DataAccessException e) {
            log.error("Database error while fetching profile", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.profile.fetch.failed")
            );

        } catch (Exception e) {
            log.error("Unexpected error while fetching profile", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.profile.fetch")
            );
        }
    }

    @Override
    @Transactional
    public ProfileResponseDto updateProfile(ProfileRequestDto profileRequestDto) {
        try {
            log.info("Updating profile for authenticated customer");

            // Validation des données d'entrée
            validateProfileRequest(profileRequestDto);

            Customer customer = getAuthenticatedCustomer();
            boolean isEmailUpdated = !customer.getEmail().equals(profileRequestDto.getEmail().trim());

            // Vérifier si l'email existe déjà (sauf pour le client actuel)
            if (isEmailUpdated && customerRepository.existsByEmailAndCustomerIdNot(profileRequestDto.getEmail(), customer.getCustomerId())) {
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.profile.email.already.exists", profileRequestDto.getEmail())
                );
            }

            // Mise à jour des informations du client
            updateCustomerFromRequest(customer, profileRequestDto);

            // Mise à jour de l'adresse si fournie
            updateCustomerAddress(customer, profileRequestDto);

            Customer savedCustomer = customerRepository.save(customer);
            ProfileResponseDto profileResponse = mapCustomerToProfileResponseDto(savedCustomer);
            profileResponse.setEmailUpdated(isEmailUpdated);

            log.info("Profile updated successfully for customer: {}", savedCustomer.getEmail());
            return profileResponse;

        } catch (BusinessException e) {
            throw e; // Relance les exceptions métier

        } catch (DataAccessException e) {
            log.error("Database error while updating profile", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.profile.update.failed")
            );

        } catch (Exception e) {
            log.error("Unexpected error while updating profile", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.profile.update")
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getAuthenticatedCustomer() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            return customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("Authenticated customer not found in database: {}", email);
                        return exceptionFactory.businessError(
                                getLocalizedMessage("error.profile.customer.not.found")
                        );
                    });

        } catch (Exception e) {
            log.error("Error while getting authenticated customer", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.profile.authentication.failed")
            );
        }
    }

    //VALIDATION
    private void validateProfileRequest(ProfileRequestDto profileRequestDto) {
        if (profileRequestDto == null) {
            throw exceptionFactory.validationError("profileRequestDto",
                    getLocalizedMessage("validation.profile.request.required"));
        }

        if (profileRequestDto.getName() == null || profileRequestDto.getName().trim().isEmpty()) {
            throw exceptionFactory.validationError("name",
                    getLocalizedMessage("validation.profile.name.required"));
        }

        if (profileRequestDto.getEmail() == null || profileRequestDto.getEmail().trim().isEmpty()) {
            throw exceptionFactory.validationError("email",
                    getLocalizedMessage("validation.profile.email.required"));
        }

        if (profileRequestDto.getMobileNumber() == null || profileRequestDto.getMobileNumber().trim().isEmpty()) {
            throw exceptionFactory.validationError("mobileNumber",
                    getLocalizedMessage("validation.profile.mobileNumber.required"));
        }

        // Validation de l'adresse complète ou absente
        if (hasPartialAddressData(profileRequestDto)) {
            throw exceptionFactory.validationError("address",
                    getLocalizedMessage("validation.profile.address.incomplete"));
        }

        // Validation du format de l'email
        if (!isValidEmail(profileRequestDto.getEmail())) {
            throw exceptionFactory.validationError("email",
                    getLocalizedMessage("validation.profile.email.invalid"));
        }

        // Validation du format du numéro de mobile
        if (!isValidMobileNumber(profileRequestDto.getMobileNumber())) {
            throw exceptionFactory.validationError("mobileNumber",
                    getLocalizedMessage("validation.profile.mobileNumber.invalid"));
        }
    }

    private void updateCustomerFromRequest(Customer customer, ProfileRequestDto profileRequestDto) {
        customer.setName(profileRequestDto.getName().trim());
        customer.setEmail(profileRequestDto.getEmail().trim());
        customer.setMobileNumber(profileRequestDto.getMobileNumber().trim());
    }

    private void updateCustomerAddress(Customer customer, ProfileRequestDto profileRequestDto) {
        if (hasCompleteAddressData(profileRequestDto)) {
            Address address = customer.getAddress();
            if (address == null) {
                address = new Address();
                address.setCustomer(customer);
            }
            address.setStreet(profileRequestDto.getStreet().trim());
            address.setCity(profileRequestDto.getCity().trim());
            address.setState(profileRequestDto.getState().trim());
            address.setPostalCode(profileRequestDto.getPostalCode().trim());
            address.setCountry(profileRequestDto.getCountry().trim());
            customer.setAddress(address);

        } else if (hasPartialAddressData(profileRequestDto)) {
            // Supprimer l'adresse si partiellement fournie
            customer.setAddress(null);
        }
        // aucune donnée d'adresse, on ne change rien
    }

    //MÉTHODES UTILITAIRES
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

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber != null && mobileNumber.matches("^\\d{10}$");
    }

    private ProfileResponseDto mapCustomerToProfileResponseDto(Customer customer) {
        ProfileResponseDto profileResponseDto = new ProfileResponseDto();
        profileResponseDto.setCustomerId(customer.getCustomerId());
        profileResponseDto.setName(customer.getName());
        profileResponseDto.setEmail(customer.getEmail());
        profileResponseDto.setMobileNumber(customer.getMobileNumber());
        profileResponseDto.setEmailUpdated(false);

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

    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}