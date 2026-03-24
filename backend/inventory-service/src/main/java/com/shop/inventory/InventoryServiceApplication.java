package com.shop.inventory;

import com.shop.inventory.model.Inventory;
import com.shop.inventory.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedInventory(InventoryRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Inventory(1L, "Wireless Headphones", 50, 10));
                repo.save(new Inventory(2L, "Running Shoes", 8, 10));   // LOW_STOCK
                repo.save(new Inventory(3L, "Leather Wallet", 5, 10));  // LOW_STOCK
                repo.save(new Inventory(4L, "Smart Watch", 30, 10));
                repo.save(new Inventory(5L, "Sunglasses", 0, 10));      // OUT_OF_STOCK
                repo.save(new Inventory(6L, "Backpack", 3, 10));        // LOW_STOCK
            }
        };
    }
}
