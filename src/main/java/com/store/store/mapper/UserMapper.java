package com.store.store.mapper;

import com.store.store.dto.AddressDto;
import com.store.store.dto.UserDto;
import com.store.store.entity.Customer;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper pour convertir Customer en UserDto.
 *
 * Centralise la logique de mapping pour éviter la duplication
 * entre AuthServiceImpl et RefreshTokenServiceImpl.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-31
 */
@Component
public class UserMapper {

    /**
     * Construit un UserDto à partir d'un Customer authentifié.
     *
     * Cette méthode est utilisée par:
     * - AuthServiceImpl.login() : Après authentification réussie
     * - RefreshTokenServiceImpl.refreshAccessToken() : Après refresh du token
     *
     * @param customer Customer authentifié
     * @param authentication Objet Authentication contenant les autorités
     * @return UserDto avec toutes les informations
     */
    public UserDto toUserDto(Customer customer, Authentication authentication) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(customer, userDto);

        // Ajouter les rôles sous forme de String séparée par virgules
        userDto.setRoles(
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","))
        );

        // Ajouter l'adresse si elle existe
        if (customer.getAddress() != null) {
            AddressDto addressDto = new AddressDto();
            BeanUtils.copyProperties(customer.getAddress(), addressDto);
            userDto.setAddress(addressDto);
        }

        return userDto;
    }
}