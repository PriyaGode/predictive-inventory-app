package com.shop.product.service;

import com.shop.product.model.Product;
import com.shop.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;
    private final CacheManager cacheManager;

    public ProductService(ProductRepository repository, CacheManager cacheManager) {
        this.repository = repository;
        this.cacheManager = cacheManager;
    }

    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  [DATABASE] Fetching ALL products from DB     ║");
        log.info("╚══════════════════════════════════════════════╝");
        return repository.findAll();
    }

    public void logCacheStatus(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null && cache.get(key) != null) {
            log.info("╔══════════════════════════════════════════════╗");
            log.info("║  [REDIS HIT] Serving '{}' key='{}' from CACHE ║", cacheName, key);
            log.info("╚══════════════════════════════════════════════╝");
        }
    }

    @Cacheable(value = "product", key = "#id")
    public Product getProductById(Long id) {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  [DATABASE] Fetching product id={} from DB    ║", id);
        log.info("╚══════════════════════════════════════════════╝");
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Cacheable(value = "productsByCategory", key = "#category")
    public List<Product> getByCategory(String category) {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  [DATABASE] Fetching category='{}' from DB    ║", category);
        log.info("╚══════════════════════════════════════════════╝");
        return repository.findByCategory(category);
    }

    @CacheEvict(value = {"products", "productsByCategory"}, allEntries = true)
    public Product createProduct(Product product) {
        log.info("║  [CACHE EVICT] Cache cleared after CREATE");
        return repository.save(product);
    }

    @CachePut(value = "product", key = "#id")
    @CacheEvict(value = {"products", "productsByCategory"}, allEntries = true)
    public Product updateProduct(Long id, Product updated) {
        log.info("║  [CACHE EVICT] Cache cleared after UPDATE id={}", id);
        Product existing = getProductById(id);
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPrice(updated.getPrice());
        existing.setRating(updated.getRating());
        existing.setImage(updated.getImage());
        existing.setDescription(updated.getDescription());
        return repository.save(existing);
    }

    @CacheEvict(value = {"products", "product", "productsByCategory"}, allEntries = true)
    public void deleteProduct(Long id) {
        log.info("║  [CACHE EVICT] Cache cleared after DELETE id={}", id);
        repository.deleteById(id);
    }
}
