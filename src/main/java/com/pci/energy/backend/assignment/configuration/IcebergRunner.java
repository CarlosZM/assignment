package com.pci.energy.backend.assignment.configuration;

import com.pci.energy.backend.assignment.repository.OrderIcebergRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class IcebergRunner implements CommandLineRunner {

    @Autowired
    private OrderIcebergRepository orderIcebergRepository;

    @Override
    public void run(String... args) throws Exception {

        this.orderIcebergRepository.initializeIcebergStructureForTable();
    }
}
