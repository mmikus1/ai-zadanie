package com.example.zadanie.api.dto;

import com.example.zadanie.entity.OrderStatus;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequest {

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private OrderStatus status;

    // NOTE: All fields optional for partial update
    // NOTE: Cannot update userId or productId (order immutable once created)
    // NOTE: total is recalculated if quantity changes
}
