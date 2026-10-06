package ru.ledvanov.model;

import ru.ledvanov.enums.OrderStatus;

import java.time.LocalDateTime;

public class Order {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private OrderStatus status;

    public Order(Long id,
                 String name,
                 LocalDateTime createdAt,
                 LocalDateTime deliveredAt,
                 OrderStatus status) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.deliveredAt = deliveredAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
