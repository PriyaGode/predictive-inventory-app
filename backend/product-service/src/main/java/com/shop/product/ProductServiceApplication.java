package com.shop.product;

import com.shop.product.model.Product;
import com.shop.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedData(ProductRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Product(null, "Wireless Headphones", "Electronics", 59.99, 4.5, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300", "High-quality wireless headphones with noise cancellation and 20hr battery life."));
                repo.save(new Product(null, "Running Shoes", "Footwear", 89.99, 4.3, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=300", "Lightweight and breathable running shoes for all terrains."));
                repo.save(new Product(null, "Leather Wallet", "Accessories", 29.99, 4.7, "https://images.unsplash.com/photo-1627123424574-724758594e93?w=300", "Slim genuine leather wallet with RFID blocking technology."));
                repo.save(new Product(null, "Smart Watch", "Electronics", 149.99, 4.6, "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300", "Feature-packed smartwatch with health tracking and GPS."));
                repo.save(new Product(null, "Sunglasses", "Accessories", 39.99, 4.2, "https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=300", "UV400 polarized sunglasses with stylish frame."));
                repo.save(new Product(null, "Backpack", "Bags", 49.99, 4.4, "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=300", "Durable 30L backpack with laptop compartment and USB charging port."));
            }
        };
    }
}
