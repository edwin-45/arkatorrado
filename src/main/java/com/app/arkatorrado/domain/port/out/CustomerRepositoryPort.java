package com.app.arkatorrado.domain.port.out;

import com.app.arkatorrado.domain.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepositoryPort {
    List<Customer> findAll();
    Optional<Customer> findById(Long id);
    Customer save(Customer customer);
    void deleteById(Long id);
    List<Customer> findByNombreStartingWith(String letra);
    boolean existsById(Long id);
}
