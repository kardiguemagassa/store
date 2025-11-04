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

/**
 * Implementation of the {@link IProfileService} interface that provides profile-related operations
 * for authenticated customers.
 *
 * This service is responsible for fetching and updating customer profile data and handling
 * authentication-related operations. It uses the {@link CustomerRepository} for database access,
 * and leverages various utility methods to manage customer information.
 *
 * It provides error handling for business and unexpected exceptions, ensuring a robust and reliable service.
 *
 * Dependencies injected:
 * - {@link CustomerRepository}: Repository for accessing Customer data.
 * - {@link ExceptionFactory}: Factory class for creating business exceptions.
 * - {@link MessageSource}: For internationalized messages.
 *
 * The service includes:
 * - Retrieval of customer profile data.
 * - Update of customer profile and address.
 * - Authentication-based customer lookup.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements IProfileService {

    private final CustomerRepository customerRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

    /**
     * Retrieves the profile of the authenticated customer.
     *
     * This method fetches the currently authenticated customer's details from the database,
     * maps them to a {@code ProfileResponseDto}, and returns them. The operation is executed
     * in a read-only transactional context. In the event of an exception, specific error messages
     * are thrown based on the type of error encountered.
     *
     * @return a {@code ProfileResponseDto} containing the details of the authenticated customer's profile
     * @throws BusinessException if there is an issue retrieving the authenticated customer, a database error occurs, or an unexpected error arises
     */
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

    /**
     * Updates the profile information of the authenticated customer.
     *
     * This method updates the details of the currently authenticated customer,
     * including their email, name, mobile number, and address information. If the
     * email provided in the request is different from the existing email, the method
     * verifies its uniqueness for all customers except the current one. After
     * updating the customer profile, it saves the changes to the database and returns
     * the updated profile details.
     *
     * @param profileRequestDto the {@code ProfileRequestDto} containing the updated profile details,
     *                          such as name, email, mobile number, and address information
     * @return a {@code ProfileResponseDto} containing the updated profile details, including
     * a flag indicating if the email was updated
     * @throws BusinessException if the email already exists for another customer, a database error occurs,
     * or an unexpected error arises
     */
    @Override
    @Transactional
    public ProfileResponseDto updateProfile(ProfileRequestDto profileRequestDto) {
        try {

            log.info("Updating profile for authenticated customer");

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

    /**
     * Retrieves the authenticated customer's details from the database.
     *
     * This method obtains the currently authenticated customer's email address from the security context,
     * fetches the corresponding customer details from the repository, and returns the customer entity.
     * If the customer is not found or an error occurs during the process, a business exception is thrown.
     *
     * @return the {@code Customer} object representing the authenticated customer
     * @throws BusinessException if the customer cannot be found or an unexpected error occurs
     */
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

    /**
     * Updates the details of a given Customer object using the data provided in a ProfileRequestDto.
     *
     * @param customer the {@code Customer} object to be updated with new data
     * @param profileRequestDto the {@code ProfileRequestDto} containing the updated customer details,
     *                          such as name, email, and mobile number
     */
    private void updateCustomerFromRequest(Customer customer, ProfileRequestDto profileRequestDto) {
        customer.setName(profileRequestDto.getName().trim());
        customer.setEmail(profileRequestDto.getEmail().trim());
        customer.setMobileNumber(profileRequestDto.getMobileNumber().trim());
    }

    /**
     * Updates the address of a given customer based on the data provided in the ProfileRequestDto.
     *
     * If complete address data is supplied, this method updates the customer's address (creating
     * a new Address object if none exists). If partial address data is provided, the customer's
     * existing address is removed. If no address data is provided, no changes are made to the customer's address.
     *
     * @param customer the {@code Customer} object whose address is to be updated
     * @param profileRequestDto the {@code ProfileRequestDto} containing the address details, which
     *                          may include street, city, state, postal code, and country
     */
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

    /**
     * Checks if the provided ProfileRequestDto contains complete address data.
     * Complete address data consists of non-null and non-empty values for street,
     * city, state, postal code, and country.
     *
     * @param dto the {@code ProfileRequestDto} object containing the address details to be validated
     * @return {@code true} if all address fields (street, city, state, postal code, and country)
     *         are provided and not empty; {@code false} otherwise
     */
    private boolean hasCompleteAddressData(ProfileRequestDto dto) {
        return dto.getStreet() != null && !dto.getStreet().trim().isEmpty() &&
                dto.getCity() != null && !dto.getCity().trim().isEmpty() &&
                dto.getState() != null && !dto.getState().trim().isEmpty() &&
                dto.getPostalCode() != null && !dto.getPostalCode().trim().isEmpty() &&
                dto.getCountry() != null && !dto.getCountry().trim().isEmpty();
    }

    /**
     * Checks if the provided {@code ProfileRequestDto} contains partial address data.
     *
     * Partial address data exists if at least one field (street, city, state, postal code,
     * or country) is non-null and non-empty but the address is not complete. An address
     * is considered complete if all fields are non-null and non-empty.
     *
     * @param dto the {@code ProfileRequestDto} object containing the address details to be checked
     * @return {@code true} if the address data is partial, meaning some but not all of the address
     * fields are provided and non-empty; {@code false} otherwise
     */
    private boolean hasPartialAddressData(ProfileRequestDto dto) {
        boolean hasSomeData = (dto.getStreet() != null && !dto.getStreet().trim().isEmpty()) ||
                (dto.getCity() != null && !dto.getCity().trim().isEmpty()) ||
                (dto.getState() != null && !dto.getState().trim().isEmpty()) ||
                (dto.getPostalCode() != null && !dto.getPostalCode().trim().isEmpty()) ||
                (dto.getCountry() != null && !dto.getCountry().trim().isEmpty());

        boolean hasAllData = hasCompleteAddressData(dto);

        return hasSomeData && !hasAllData;
    }

    /**
     * Maps a {@code Customer} entity to a {@code ProfileResponseDto}.
     *
     * This method converts the details of a {@code Customer} object, including
     * basic customer information and address details, into a {@code ProfileResponseDto}.
     * If the customer has an associated address, its details are also mapped to an
     * {@code AddressDto} and included in the resulting {@code ProfileResponseDto}.
     *
     * @param customer the {@code Customer} entity containing the data to be mapped
     * @return a {@code ProfileResponseDto} containing the mapped customer details,
     *         including optional address information
     */
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

    /**
     * Retrieves a localized message based on the provided message code and arguments.
     *
     * This method uses a message source to fetch a localized message corresponding
     * to the given code in the current locale. Any additional arguments can be
     * supplied and will be interpolated into the message if placeholders are
     * present in the message template.
     *
     * @param code the message code identifying the specific message template
     * @param args optional arguments to be interpolated into the message template
     * @return the localized message for the provided code and arguments in the current locale
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}