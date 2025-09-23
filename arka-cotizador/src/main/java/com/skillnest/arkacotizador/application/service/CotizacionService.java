package com.skillnest.arkacotizador.application.service;

import com.skillnest.arkacotizador.domain.model.CotizacionRequest;
import com.skillnest.arkacotizador.domain.model.CotizacionResponse;
import reactor.core.publisher.Mono;

public interface CotizacionService {
    Mono<CotizacionResponse> generarCotizacion(CotizacionRequest request);
    Mono<CotizacionResponse> obtenerCotizacion(String cotizacionId);
}
