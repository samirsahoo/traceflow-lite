package io.traceflow.demo.service;

import io.traceflow.demo.config.ServerPortHolder;
import io.traceflow.demo.domain.Order;
import io.traceflow.demo.repository.OrderRepository;
import io.traceflow.demo.web.dto.OrderRequest;
import io.traceflow.demo.web.dto.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * The order write path exercises every Section 20 scenario in one flow: a
 * downstream REST call (product validation, over loopback HTTP so the demo
 * runs as a single process), then a real JDBC write against H2.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RestClient.Builder restClientBuilder;
    private final ServerPortHolder serverPortHolder;
    private volatile RestClient restClient;

    public OrderService(OrderRepository orderRepository, RestClient.Builder restClientBuilder,
                         ServerPortHolder serverPortHolder) {
        this.orderRepository = orderRepository;
        this.restClientBuilder = restClientBuilder;
        this.serverPortHolder = serverPortHolder;
    }

    /** Built on first use: the real port is only known once the web server has started. */
    private RestClient restClient() {
        RestClient client = restClient;
        if (client == null) {
            synchronized (this) {
                client = restClient;
                if (client == null) {
                    client = restClientBuilder.baseUrl("http://localhost:" + serverPortHolder.port()).build();
                    restClient = client;
                }
            }
        }
        return client;
    }

    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order " + id + " not found"));
    }

    public Order createOrder(OrderRequest request) {
        ProductResponse product = validateProduct(request.product());
        log.info("Validated product before order creation: {}", product.name());

        Order order = new Order(request.product(), request.quantity(), "CREATED");
        return orderRepository.save(order);
    }

    private ProductResponse validateProduct(String productId) {
        try {
            return restClient().get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NoSuchElementException("Product " + productId + " does not exist");
        }
    }
}
