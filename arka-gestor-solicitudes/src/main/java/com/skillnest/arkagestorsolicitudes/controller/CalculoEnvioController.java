package com.skillnest.arkagestorsolicitudes.controller;

import com.skillnest.arkagestorsolicitudes.domain.model.CalculoEnvio;
import com.skillnest.arkagestorsolicitudes.domain.model.EstadoCalculo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el cálculo de envíos con Circuit Breaker
 * Aplicando seguridad basada en roles
 * Los endpoints disponibles serán:
 * POST /api/calculos/envio - Calcular envío básico
 * POST /api/calculos/envio-multiple - Comparar múltiples proveedores
 * POST /api/calculos/envio-con-fallback - Con manejo de fallos
 * POST /api/calculos/solicitud/{solicitudId}/calcular-envio - Para solicitud específica
 * GET /api/calculos - Obtener cálculo por ID
 * GET /api/calculos/estado/{calculoId} - Obtener solo el estado
 */
@RestController
@RequestMapping("/api/calculos")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CalculoEnvioController {

    @PostMapping("/envio")
    @ResponseStatus(HttpStatus.OK)
    public Mono<CalculoEnvio> calcularEnvio(@RequestBody CalculoEnvio solicitudCalculo) {
        return Mono.fromCallable(() -> {
            // Simular cálculo de envío usando los métodos de la clase CalculoEnvio
            String calculoId = UUID.randomUUID().toString();

            // Simular un cálculo exitoso
            CalculoEnvio resultado = CalculoEnvio.exitoso(
                    calculoId,
                    BigDecimal.valueOf(25.50),
                    5,
                    "PROVEEDOR_PRINCIPAL"
            );

            resultado.setOrigen(solicitudCalculo.getOrigen());
            resultado.setDestino(solicitudCalculo.getDestino());
            resultado.setPeso(solicitudCalculo.getPeso());
            resultado.setDimensiones(solicitudCalculo.getDimensiones());

            return resultado;
        });
    }

    @PostMapping("/envio-multiple")
    public Flux<CalculoEnvio> calcularEnvioMultiplesProveedores(@RequestBody CalculoEnvio solicitudCalculo) {
        return Flux.fromIterable(List.of(
                crearCalculoProveedor(solicitudCalculo, "PROVEEDOR_A", BigDecimal.valueOf(25.50), 5),
                crearCalculoProveedor(solicitudCalculo, "PROVEEDOR_B", BigDecimal.valueOf(22.00), 7),
                crearCalculoProveedor(solicitudCalculo, "PROVEEDOR_C", BigDecimal.valueOf(28.75), 3)
        ));
    }

    @PostMapping("/envio-con-fallback")
    public Mono<CalculoEnvio> calcularEnvioConFallback(@RequestBody CalculoEnvio solicitudCalculo) {
        return Mono.fromCallable(() -> {
            // Simular un servicio que puede fallar y usar fallback
            boolean servicioDisponible = Math.random() > 0.3; // 70% de éxito

            if (servicioDisponible) {
                String calculoId = UUID.randomUUID().toString();
                CalculoEnvio resultado = CalculoEnvio.exitoso(
                        calculoId,
                        BigDecimal.valueOf(20.00),
                        4,
                        "SERVICIO_PRINCIPAL"
                );
                resultado.setOrigen(solicitudCalculo.getOrigen());
                resultado.setDestino(solicitudCalculo.getDestino());
                resultado.setPeso(solicitudCalculo.getPeso());
                resultado.setDimensiones(solicitudCalculo.getDimensiones());
                return resultado;
            } else {
                // Usar el método fallback de la clase CalculoEnvio
                CalculoEnvio fallback = CalculoEnvio.fallback("Servicio principal no disponible, usando valores por defecto");
                fallback.setOrigen(solicitudCalculo.getOrigen());
                fallback.setDestino(solicitudCalculo.getDestino());
                fallback.setPeso(solicitudCalculo.getPeso());
                fallback.setDimensiones(solicitudCalculo.getDimensiones());
                return fallback;
            }
        });
    }

    @PostMapping("/solicitud/{solicitudId}/calcular-envio")
    public Mono<CalculoEnvio> calcularEnvioParaSolicitud(
            @PathVariable String solicitudId,
            @RequestBody CalculoEnvio parametrosEnvio) {

        return Mono.fromCallable(() -> {
            String calculoId = UUID.randomUUID().toString();
            CalculoEnvio resultado = CalculoEnvio.exitoso(
                    calculoId,
                    BigDecimal.valueOf(18.99),
                    6,
                    "PROVEEDOR_SOLICITUD"
            );

            resultado.setOrigen(parametrosEnvio.getOrigen());
            resultado.setDestino(parametrosEnvio.getDestino());
            resultado.setPeso(parametrosEnvio.getPeso());
            resultado.setDimensiones(parametrosEnvio.getDimensiones());

            return resultado;
        });
    }

    @GetMapping("/{calculoId}")
    public Mono<CalculoEnvio> obtenerCalculoPorId(@PathVariable String calculoId) {
        return Mono.fromCallable(() -> {
            // Simular búsqueda por ID
            CalculoEnvio calculo = CalculoEnvio.exitoso(
                    calculoId,
                    BigDecimal.valueOf(15.75),
                    4,
                    "PROVEEDOR_CONSULTADO"
            );
            calculo.setOrigen("Ciudad A");
            calculo.setDestino("Ciudad B");
            calculo.setPeso(BigDecimal.valueOf(2.5));
            calculo.setDimensiones("30x20x15 cm");

            return calculo;
        });
    }

    @GetMapping("/estado/{calculoId}")
    public Mono<EstadoCalculo> obtenerEstadoCalculo(@PathVariable String calculoId) {
        return Mono.fromCallable(() -> {
            // Simular diferentes estados
            EstadoCalculo[] estados = EstadoCalculo.values();
            return estados[(int) (Math.random() * estados.length)];
        });
    }

    @PostMapping("/simular-error")
    public Mono<CalculoEnvio> simularError(@RequestBody CalculoEnvio solicitudCalculo) {
        return Mono.fromCallable(() ->
                CalculoEnvio.error("Error simulado en el cálculo de envío")
        );
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Servicio de Cálculo de Envío funcionando correctamente");
    }

    // Método auxiliar
    private CalculoEnvio crearCalculoProveedor(CalculoEnvio base, String proveedor, BigDecimal costo, Integer tiempo) {
        String calculoId = UUID.randomUUID().toString();
        CalculoEnvio calculo = CalculoEnvio.exitoso(calculoId, costo, tiempo, proveedor);

        calculo.setOrigen(base.getOrigen());
        calculo.setDestino(base.getDestino());
        calculo.setPeso(base.getPeso());
        calculo.setDimensiones(base.getDimensiones());

        return calculo;
    }

}
