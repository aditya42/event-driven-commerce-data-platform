package dev.aditya.data.order.web;

import dev.aditya.data.order.model.*;
import dev.aditya.data.order.service.OrdersService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final OrdersService service;

    public OrderController(OrdersService service) { this.service = service; }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request) { return service.create(request); }

    @GetMapping("/orders")
    public List<OrderResponse> list() { return service.list(); }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommerceEvent emit(@Valid @RequestBody DomainEventRequest request) { return service.emit(request); }
}
