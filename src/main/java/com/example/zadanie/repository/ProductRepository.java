package com.example.zadanie.repository;

import com.example.zadanie.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // JpaRepository provides:
    // - save(Product product)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - existsById(Long id)

    // Custom query method (optional, for future use)
    Optional<Product> findByName(String name);
}