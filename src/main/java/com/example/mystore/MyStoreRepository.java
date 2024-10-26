package com.example.mystore;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MyStoreRepository extends JpaRepository<Product, Integer>{
    
}
