package dev.aditya.data.order.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record OrderRequest(
        @NotBlank String customerId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotEmpty List<@Valid OrderItemRequest> items) {
}
