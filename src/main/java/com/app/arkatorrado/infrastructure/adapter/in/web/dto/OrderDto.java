package com.app.arkatorrado.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class OrderDto {

    private Long id;
    private Long clienteId;
    private LocalDateTime fecha;
    private BigDecimal total;
    private Set<Long> productosIds = new HashSet<>();

    // Constructor vacío
    public OrderDto() {}

    // Constructor completo
    public OrderDto(Long id, Long clienteId, LocalDateTime fecha, BigDecimal total, Set<Long> productosIds) {
        this.id = id;
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.total = total;
        this.productosIds = productosIds;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Set<Long> getProductosIds() {
        return productosIds;
    }

    public void setProductosIds(Set<Long> productosIds) {
        this.productosIds = productosIds != null ? productosIds : new HashSet<>();
    }
}
