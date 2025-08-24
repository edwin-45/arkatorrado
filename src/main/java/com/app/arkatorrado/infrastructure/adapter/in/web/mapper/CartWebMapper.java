package com.app.arkatorrado.infrastructure.adapter.in.web.mapper;

import com.app.arkatorrado.domain.model.Cart;
import com.app.arkatorrado.domain.model.Customer;
import com.app.arkatorrado.infrastructure.adapter.in.web.dto.CartDto;

public class CartWebMapper {
    /**
     * Convierte un objeto Cart del dominio a un CartDto
     */
    public static CartDto toDto(Cart cart) {
        if (cart == null) {
            return null;
        }

        CartDto cartDto = new CartDto();
        cartDto.setId(cart.getId());
        cartDto.setClienteId(cart.getCliente() != null ? cart.getCliente().getId() : null);
        cartDto.setFechaCreacion(cart.getFechaCreacion());
        cartDto.setEstado(cart.getEstado());

        return cartDto;
    }

    /**
     * Convierte un CartDto a un objeto Cart del dominio
     * Requiere un objeto Customer completo para la conversión
     */
    public static Cart toDomain(CartDto cartDto, Customer cliente) {
        if (cartDto == null) {
            return null;
        }

        Cart cart = new Cart();
        cart.setId(cartDto.getId());
        cart.setCliente(cliente);
        cart.setFechaCreacion(cartDto.getFechaCreacion());
        cart.setEstado(cartDto.getEstado());

        return cart;
    }
}
