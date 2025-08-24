package com.app.arkatorrado.infrastructure.adapter.in.web;

import com.app.arkatorrado.application.usecase.CartApplicationService;
import com.app.arkatorrado.application.usecase.CustomerApplicationService;
import com.app.arkatorrado.domain.model.Cart;
import com.app.arkatorrado.domain.model.Customer;
import com.app.arkatorrado.domain.port.in.CartUseCase;
import com.app.arkatorrado.domain.port.in.CustomerUseCase;
import com.app.arkatorrado.infrastructure.adapter.in.web.dto.CartDto;
import com.app.arkatorrado.infrastructure.adapter.in.web.mapper.CartWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/carritos")
public class CartController {

    private final CartUseCase cartService;
    private final CustomerUseCase customerService;

    public CartController(CartUseCase cartService, CustomerUseCase customerService) {
        this.cartService = cartService;
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<CartDto>> obtenerTodos() {
        List<CartDto> carritos = cartService.getAllCarts().stream()
                .map(CartWebMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(carritos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartDto> obtenerPorId(@PathVariable Long id) {
        try {
            Cart cart = cartService.getCartById(id);
            return ResponseEntity.ok(CartWebMapper.toDto(cart));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<CartDto> crear(@RequestBody CartDto cartDto) {
        try {
            Customer cliente = customerService.getCustomerById(cartDto.getClienteId());
            Cart cart = CartWebMapper.toDomain(cartDto, cliente);
            Cart savedCart = cartService.createCart(cart);
            return new ResponseEntity<>(CartWebMapper.toDto(savedCart), HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartDto> actualizar(@PathVariable Long id, @RequestBody CartDto cartDto) {
        try {
            Customer cliente = customerService.getCustomerById(cartDto.getClienteId());
            Cart cart = CartWebMapper.toDomain(cartDto, cliente);
            Cart updatedCart = cartService.updateCart(id, cart);
            return ResponseEntity.ok(CartWebMapper.toDto(updatedCart));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            cartService.deleteCart(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/abandonados")
    public ResponseEntity<List<CartDto>> obtenerCarritosAbandonados() {
        List<CartDto> carritos = cartService.getAbandonedCarts().stream()
                .map(CartWebMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(carritos);
    }
}
