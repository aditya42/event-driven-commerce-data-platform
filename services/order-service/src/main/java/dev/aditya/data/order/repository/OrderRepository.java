package dev.aditya.data.order.repository;

import dev.aditya.data.order.model.OrderItemRequest;
import dev.aditya.data.order.model.OrderResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String orderId, String customerId, String currency, BigDecimal total, Instant createdAt, List<OrderItemRequest> items) {
        jdbc.update("INSERT INTO orders(order_id, customer_id, currency, total_amount, status, created_at) VALUES (?,?,?,?,?,?)",
                orderId, customerId, currency, total, "CREATED", Timestamp.from(createdAt));
        for (int i = 0; i < items.size(); i++) {
            OrderItemRequest item = items.get(i);
            jdbc.update("INSERT INTO order_items(order_id, line_no, product_id, quantity, unit_price) VALUES (?,?,?,?,?)",
                    orderId, i + 1, item.productId(), item.quantity(), item.unitPrice());
        }
    }

    public List<OrderResponse> findAll() {
        return jdbc.query("SELECT order_id, customer_id, currency, total_amount, status, created_at FROM orders ORDER BY created_at DESC",
                (rs, rowNum) -> new OrderResponse(
                        rs.getString("order_id"), rs.getString("customer_id"), rs.getString("currency"),
                        rs.getBigDecimal("total_amount"), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()));
    }
}
