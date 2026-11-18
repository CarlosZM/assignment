package com.pci.energy.backend.assignment.business;

import com.pci.energy.backend.assignment.repository.OrderIcebergRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderIcebergRepository orderIcebergRepository;

    public Order create(Order order) {

        this.orderIcebergRepository.upsertOrders(Collections.singletonList(order));

        return order;
    }

    public List<Order> getAll() {
        return this.orderIcebergRepository.findAll();
    }

    public List<Order> bulk(String fileName) {
        return this.orderIcebergRepository.bulk(fileName);
    }
}
