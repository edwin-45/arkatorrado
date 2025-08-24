package com.app.arkatorrado.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;

public class CartDto {
    private Long id;
    private Long clienteId;  // Simplificamos usando solo el ID del cliente
    private LocalDateTime fechaCreacion;
    private String estado;

    // Constructor vacío
    public CartDto() {}

    // Constructor completo
    public CartDto(Long id, Long clienteId, LocalDateTime fechaCreacion, String estado) {
        this.id = id;
        this.clienteId = clienteId;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
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

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
