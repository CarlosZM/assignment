package com.pci.energy.backend.assignment.business;

import java.sql.Date;
import java.time.LocalDate;

public record Order(
        Integer order_id,
        String customer_id,
        String product,
        String category,
        Integer quantity,
        Float unit_price,
        Date order_date,
        String country
) {
}
