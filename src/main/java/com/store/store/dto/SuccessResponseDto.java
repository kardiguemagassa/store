package com.store.store.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponseDto {
    private String message;
    private int status;
    private Object data;

    public static SuccessResponseDto of(String message, int status) {
        return SuccessResponseDto.builder()
                .message(message)
                .status(status)
                .build();
    }

    public static SuccessResponseDto of(String message, int status, Object data) {
        return SuccessResponseDto.builder()
                .message(message)
                .status(status)
                .data(data)
                .build();
    }
}