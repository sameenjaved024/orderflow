package com.orderflow.order.mapper;

import com.orderflow.order.domain.Order;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "status",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)

    Order toEntity(CreateOrderRequest request);
    OrderResponse toResponse(Order order);
}
