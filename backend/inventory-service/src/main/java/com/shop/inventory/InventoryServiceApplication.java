package com.shop.inventory;

import com.shop.inventory.alert.AlertService;
import com.shop.inventory.alert.CategoryThreshold;
import com.shop.inventory.forecast.ForecastRequest;
import com.shop.inventory.forecast.ForecastService;
import com.shop.inventory.model.Inventory;
import com.shop.inventory.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedData(InventoryRepository repo, ForecastService forecastService, AlertService alertService) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Inventory(1L, "Wireless Headphones", 50, 10));
                repo.save(new Inventory(2L, "Running Shoes",       8,  10));
                repo.save(new Inventory(3L, "Leather Wallet",      5,  10));
                repo.save(new Inventory(4L, "Smart Watch",         30, 10));
                repo.save(new Inventory(5L, "Sunglasses",          0,  10));
                repo.save(new Inventory(6L, "Backpack",            3,  10));
            }

            // Seed category thresholds
            alertService.saveThreshold(new CategoryThreshold("DEFAULT",     10,  0, 200));
            alertService.saveThreshold(new CategoryThreshold("Headphones",  15,  0, 150));
            alertService.saveThreshold(new CategoryThreshold("Shoes",       12,  0, 180));
            alertService.saveThreshold(new CategoryThreshold("Watch",       10,  0, 100));
            alertService.saveThreshold(new CategoryThreshold("Wallet",       8,  0, 120));
            alertService.saveThreshold(new CategoryThreshold("Sunglasses",  10,  0, 160));
            alertService.saveThreshold(new CategoryThreshold("Backpack",    10,  0, 140));

            // Seed sample ML forecasts
            seedForecast(forecastService, 1L, "Wireless Headphones", 120.0, 14, 0.91);
            seedForecast(forecastService, 2L, "Running Shoes",        45.0,  7, 0.85);
            seedForecast(forecastService, 3L, "Leather Wallet",       30.0,  7, 0.78);
            seedForecast(forecastService, 4L, "Smart Watch",          60.0, 14, 0.88);
            seedForecast(forecastService, 5L, "Sunglasses",           80.0,  7, 0.72);
            seedForecast(forecastService, 6L, "Backpack",             55.0, 14, 0.65);

            // Run initial alert scan after seeding
            alertService.runFullScan();
        };
    }

    private void seedForecast(ForecastService svc, Long productId, String name,
                               double demand, int windowDays, double confidence) {
        ForecastRequest req = new ForecastRequest();
        req.setProductId(productId);
        req.setProductName(name);
        req.setPredictedDemand(demand);
        req.setForecastWindowDays(windowDays);
        req.setConfidenceScore(confidence);
        svc.ingestForecast(req);
    }
}
