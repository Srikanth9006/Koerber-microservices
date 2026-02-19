package com.koerber.order.service;

import com.koerber.order.dto.OrderRequest;
import com.koerber.order.dto.OrderResponse;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest request);
}
