package com.store.store.mapper;

import com.store.store.dto.address.AddressDto;
import com.store.store.dto.user.UserDto;
import com.store.store.entity.Customer;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready
 * @since 2025-01-06
 */
@Component
public class UserMapper {


    public UserDto toUserDto(Customer customer, Authentication authentication) {
        UserDto userDto = new UserDto();

        // 1. Copie automatique des champs simples
        BeanUtils.copyProperties(customer, userDto);

        // 2. Extraction des rôles depuis Authentication
        Set<String> roleSet = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        userDto.setRoleSet(roleSet);

        // 3. Format CSV pour rétrocompatibilité (deprecated, utiliser roleSet)
        userDto.setRoles(String.join(",", roleSet));

        // 4. Mapping de l'adresse si elle existe
        if (customer.getAddress() != null) {
            AddressDto addressDto = new AddressDto();
            BeanUtils.copyProperties(customer.getAddress(), addressDto);
            userDto.setAddress(addressDto);
        }

        return userDto;
    }

    public UserDto toUserDtoBasic(Customer customer) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(customer, userDto);

        // Mapping de l'adresse si elle existe
        if (customer.getAddress() != null) {
            AddressDto addressDto = new AddressDto();
            BeanUtils.copyProperties(customer.getAddress(), addressDto);
            userDto.setAddress(addressDto);
        }

        return userDto;
    }
}