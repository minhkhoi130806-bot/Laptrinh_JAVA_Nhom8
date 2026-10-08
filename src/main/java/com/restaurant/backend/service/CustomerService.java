package com.restaurant.backend.service;

import com.restaurant.backend.entity.Customer;

import java.util.List;

public interface CustomerService {
    Customer create(Customer customer);

    List<Customer> getAll();

    Customer getById(Long id);

    Customer update(Long id, Customer customer);

    void delete(Long id);
}
