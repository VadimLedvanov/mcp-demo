package ru.ledvanov.service;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;
import ru.ledvanov.enums.OrderStatus;
import ru.ledvanov.model.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderService {
    private final List<Order> orders;

    public OrderService() {
        this.orders = new ArrayList<>();
    }

    @McpTool(description = "creates new order")
    public Order createOrder(
            McpSyncRequestContext context,
            @McpToolParam(description = "name of order") String name) throws InterruptedException {
        context.progress(p -> p
                .progress(0.0)
                .total(1.0)
                .message("Создаём заказ"));

        Thread.sleep(1000);

        Long id = orders.isEmpty() ? 1L : orders.getLast().getId() + 1;
        LocalDateTime createdAt = LocalDateTime.now();
        OrderStatus status = OrderStatus.NEW;

        Order newOrder = new Order(id, name, createdAt, null, status);

        context.progress(p -> p
                .progress(0.5)
                .total(1.0)
                .message("Добавляем заказ в список"));

        Thread.sleep(1000);

        orders.add(newOrder);

        context.progress(p -> p
                .progress(1.0)
                .total(1.0)
                .message("Заказ №" + id +  " успешно создан"));

        return newOrder;
    }

    @McpTool(description = "gets order by order id")
    public Order getOrderById(@McpToolParam(description = "order id") Long id) {
        return orders.stream()
                .filter(o -> o.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Заказ с ID " + id + " не найден"));
    }

    @McpTool(description = "gets all orders")
    public List<Order> getOrders() {
        return orders;
    }
}
