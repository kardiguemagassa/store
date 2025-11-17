package com.store.store.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddressDto {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size.max}")
    private String street;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size.max}")
    private String city;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size.max}")
    private String state;

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "^[0-9]{5}$", message = "{validation.pattern}")
    private String postalCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size.max}")
    private String country;
}
