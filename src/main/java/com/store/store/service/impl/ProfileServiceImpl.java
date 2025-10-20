package com.store.store.service.impl;

import com.store.store.dto.AddressDto;
import com.store.store.dto.ProfileRequestDto;
import com.store.store.dto.ProfileResponseDto;
import com.store.store.entity.Address;
import com.store.store.entity.Customer;
import com.store.store.repository.CustomerRepository;
import com.store.store.service.IProfileService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements IProfileService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile() {
        Customer customer = getAuthenticatedCustomer();
        return mapCustomerToProfileResponseDto(customer);
    }

    @Override
    @Transactional
    public ProfileResponseDto updateProfile(ProfileRequestDto profileRequestDto) {
        Customer customer = getAuthenticatedCustomer();
        boolean isEmailUpdated = !customer.getEmail().equals(profileRequestDto.getEmail().trim());

        // Mise à jour des informations du client
        customer.setName(profileRequestDto.getName());
        customer.setEmail(profileRequestDto.getEmail());
        customer.setMobileNumber(profileRequestDto.getMobileNumber());

        // Mise à jour de l'adresse si elle est fournie
        if (hasAddressData(profileRequestDto)) {
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
        }

        customer = customerRepository.save(customer);
        ProfileResponseDto profileResponseDto = mapCustomerToProfileResponseDto(customer);
        profileResponseDto.setEmailUpdated(isEmailUpdated);
        return profileResponseDto;
    }

    /**
     * Vérifie si au moins un champ d'adresse est fourni non vide
     */
    private boolean hasAddressData(ProfileRequestDto dto) {
        return (dto.getStreet() != null && !dto.getStreet().trim().isEmpty()) ||
                (dto.getCity() != null && !dto.getCity().trim().isEmpty()) ||
                (dto.getState() != null && !dto.getState().trim().isEmpty()) ||
                (dto.getPostalCode() != null && !dto.getPostalCode().trim().isEmpty()) ||
                (dto.getCountry() != null && !dto.getCountry().trim().isEmpty());
    }

    /**
     * Valide que tous les champs d'adresse sont fournis non vides
     */
    private void validateCompleteAddress(ProfileRequestDto dto) {
        boolean hasIncompleteAddress =
                (dto.getStreet() == null || dto.getStreet().trim().isEmpty()) ||
                        (dto.getCity() == null || dto.getCity().trim().isEmpty()) ||
                        (dto.getState() == null || dto.getState().trim().isEmpty()) ||
                        (dto.getPostalCode() == null || dto.getPostalCode().trim().isEmpty()) ||
                        (dto.getCountry() == null || dto.getCountry().trim().isEmpty());

        if (hasIncompleteAddress) {
            throw new IllegalArgumentException(
                    "Si vous fournissez une adresse, tous les champs (rue, ville, département, code postal, pays) sont obligatoires"
            );
        }
    }

    public Customer getAuthenticatedCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
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
}