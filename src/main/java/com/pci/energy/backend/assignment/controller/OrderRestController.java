package com.pci.energy.backend.assignment.controller;


import com.pci.energy.backend.assignment.business.Order;
import com.pci.energy.backend.assignment.business.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderRestController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/bulk/{file_name}")
    @ResponseStatus(HttpStatus.OK)
    public List<Order> bulkInsert(@PathVariable("file_name") String fileName) {

        return this.orderService.bulk(fileName);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody Order order) {
        return this.orderService.create(order);
    }

    @GetMapping("/health")
    @ResponseStatus(HttpStatus.OK)
    public Map<String,Object> createOrder() {
        return Map.of("status", true);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Order> getAll() {
        return this.orderService.getAll();
    }
}
