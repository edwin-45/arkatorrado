package com.skillnest.arkagestorsolicitudes.application.service;

import com.skillnest.arkagestorsolicitudes.domain.model.RespuestaProveedor;
import com.skillnest.arkagestorsolicitudes.domain.model.SolicitudProveedor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GestorSolicitudesService {

    Mono<SolicitudProveedor> crearSolicitud(SolicitudProveedor solicitud);
    Mono<SolicitudProveedor> enviarSolicitudAProveedor(String solicitudId, String proveedorId);
    Flux<RespuestaProveedor> obtenerRespuestasProveedor(String solicitudId);
    Mono<RespuestaProveedor> procesarRespuestaProveedor(RespuestaProveedor respuesta);

}
