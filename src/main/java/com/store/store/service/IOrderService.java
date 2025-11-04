package com.store.store.service;

import com.store.store.dto.OrderRequestDto;
import com.store.store.dto.OrderResponseDto;

import java.util.List;

/**
 * Service interface for managing orders. Provides methods to create,
 * retrieve, and update order information.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
public interface IOrderService {

    void createOrder(OrderRequestDto orderRequest);
    List<OrderResponseDto> getCustomerOrders();

    List<OrderResponseDto> getAllPendingOrders();

    void updateOrderStatus(Long orderId, String orderStatus);
}
