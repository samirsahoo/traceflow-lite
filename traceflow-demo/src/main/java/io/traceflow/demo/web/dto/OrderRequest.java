package io.traceflow.demo.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderRequest(@NotBlank String product, @Min(1) int quantity) {
}
