package com.app.arkatorrado;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PruebaPeticiones {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllProductsReturnsOk() throws Exception {
        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsersReturnsOk() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCategoriesReturnsOk() throws Exception {
        mockMvc.perform(get("/categorias"))
                .andExpect(status().isOk());
    }

    @Test
    void searchProductsReturnsOk() throws Exception {
        mockMvc.perform(get("/productos/buscar").param("term", "test"))
                .andExpect(status().isOk());
    }

    @Test
    void searchUsersReturnsOk() throws Exception {
        mockMvc.perform(get("/usuarios/buscar").param("nombre", "test"))
                .andExpect(status().isOk());
    }
}
