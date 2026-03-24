package com.shop.order.service;

import com.shop.order.model.Order;
import com.shop.order.model.OrderItem;
import com.shop.order.model.OrderRequest;
import com.shop.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "orders")
    public List<Order> getAllOrders() {
        log.info(">>> [DB FETCH] Loading ALL orders from DATABASE");
        List<Order> orders = repository.findAll();
        log.info(">>> [DB FETCH] Fetched {} orders from DATABASE", orders.size());
        return orders;
    }

    @Cacheable(value = "order", key = "#id")
    public Order getOrderById(Long id) {
        log.info(">>> [DB FETCH] Loading order id={} from DATABASE", id);
        Order order = repository.findById(id).orElseThrow(() -> new RuntimeException("Order not found: " + id));
        log.info(">>> [DB FETCH] Fetched order id={} status='{}' from DATABASE", id, order.getStatus());
        return order;
    }

    @Cacheable(value = "ordersByEmail", key = "#email")
    public List<Order> getOrdersByEmail(String email) {
        log.info(">>> [DB FETCH] Loading orders for email='{}' from DATABASE", email);
        List<Order> orders = repository.findByCustomerEmail(email);
        log.info(">>> [DB FETCH] Fetched {} orders for email='{}' from DATABASE", orders.size(), email);
        return orders;
    }

    @CacheEvict(value = {"orders", "ordersByEmail"}, allEntries = true)
    public Order placeOrder(OrderRequest request) {
        log.info(">>> [CACHE EVICT] Clearing orders cache after new ORDER placed for '{}'", request.getCustomerEmail());
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setStatus("PENDING");

        List<OrderItem> items = request.getItems().stream().map(i -> {
            OrderItem item = new OrderItem();
            item.setProductId(i.getProductId());
            item.setProductName(i.getProductName());
            item.setPrice(i.getPrice());
            item.setQuantity(i.getQuantity());
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);
        order.setTotalAmount(items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum());

        return repository.save(order);
    }

    @CacheEvict(value = {"orders", "order", "ordersByEmail"}, allEntries = true)
    public Order updateStatus(Long id, String status) {
        log.info(">>> [CACHE EVICT] Clearing orders cache after STATUS UPDATE id={} to '{}'", id, status);
        Order order = repository.findById(id).orElseThrow(() -> new RuntimeException("Order not found: " + id));
        order.setStatus(status);
        return repository.save(order);
    }
}
