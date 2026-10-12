package com.example.mofawid.DTO.BillingDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillingRequestDTO {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal totalAmount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amountPaid;

    private String description;
}