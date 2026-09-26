package io.traceflow.demo.web;

import io.traceflow.demo.web.dto.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Static catalog - just enough for {@code OrderService} to have a real
 * downstream REST call to make (Section 20).
 */
@RestController
public class ProductController {

    private static final Map<String, ProductResponse> PRODUCTS = Map.of(
            "widget", new ProductResponse("widget", "Widget", 9.99),
            "gadget", new ProductResponse("gadget", "Gadget", 19.99),
            "gizmo", new ProductResponse("gizmo", "Gizmo", 29.99)
    );

    @GetMapping("/api/products")
    public List<ProductResponse> listProducts() {
        return List.copyOf(PRODUCTS.values());
    }

    @GetMapping("/api/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String id) {
        ProductResponse product = PRODUCTS.get(id);
        return product == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(product);
    }
}
