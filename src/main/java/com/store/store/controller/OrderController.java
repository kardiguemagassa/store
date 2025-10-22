package com.store.store.controller;

import com.store.store.dto.OrderRequestDto;
import com.store.store.dto.OrderResponseDto;
import com.store.store.dto.SuccessResponseDto;
import com.store.store.service.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;
    private final MessageSource messageSource;

    @PostMapping
    public ResponseEntity<SuccessResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto requestDto) {

        orderService.createOrder(requestDto);

        return ResponseEntity.ok(SuccessResponseDto.of(
                getLocalizedMessage("success.order.created"),
                HttpStatus.OK.value()
        ));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getCustomerOrders() {
        return ResponseEntity.ok(orderService.getCustomerOrders());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<OrderResponseDto>> getPendingOrders() {
        return ResponseEntity.ok(orderService.getAllPendingOrders());
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<SuccessResponseDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {

        orderService.updateOrderStatus(orderId, status);

        return ResponseEntity.ok(SuccessResponseDto.of(
                getLocalizedMessage("success.order.status.updated"),
                HttpStatus.OK.value()
        ));
    }

    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
