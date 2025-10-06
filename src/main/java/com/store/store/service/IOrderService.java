package com.store.store.service;

import com.store.store.dto.OrderRequestDto;

public interface IOrderService {

    void createOrder(OrderRequestDto orderRequest);
}
