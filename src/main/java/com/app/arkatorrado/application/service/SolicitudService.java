package com.app.arkatorrado.application.service;

import com.app.arkatorrado.domain.model.Solicitud;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interfaz de servicio para gestión de solicitudes
 * Define las operaciones reactivas para el dominio de solicitudes
 */
public interface SolicitudService {
    
    /**
     * Crear una nueva solicitud
     */
    Mono<Solicitud> crearSolicitud(Solicitud solicitud);
    
    /**
     * Obtener solicitud por ID
     */
    Mono<Solicitud> obtenerPorId(Long id);
    
    /**
     * Obtener todas las solicitudes
     */
    Flux<Solicitud> obtenerTodas();
    
    /**
     * Obtener solicitudes por estado
     */
    Flux<Solicitud> obtenerPorEstado(String estado);
    
    /**
     * Obtener solicitudes por cliente
     */
    Flux<Solicitud> obtenerPorCliente(Long clienteId);
    
    /**
     * Actualizar solicitud
     */
    Mono<Solicitud> actualizar(Long id, Solicitud solicitud);
    
    /**
     * Eliminar solicitud
     */
    Mono<Void> eliminar(Long id);
    
    /**
     * Confirmar solicitud
     */
    Mono<Solicitud> confirmar(Long id);
    
    /**
     * Cancelar solicitud
     */
    Mono<Solicitud> cancelar(Long id);
}
