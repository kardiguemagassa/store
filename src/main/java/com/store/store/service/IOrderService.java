package com.store.store.service;

import com.store.store.dto.OrderRequestDto;
import com.store.store.dto.OrderResponseDto;

import java.util.List;

public interface IOrderService {

    void createOrder(OrderRequestDto orderRequest);
    List<OrderResponseDto> getCustomerOrders();

    List<OrderResponseDto> getAllPendingOrders();

    void updateOrderStatus(Long orderId, String orderStatus);
}
