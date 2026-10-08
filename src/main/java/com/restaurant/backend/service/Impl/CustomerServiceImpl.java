package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.CustomerRepository;
import com.restaurant.backend.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public Customer create(Customer customer) {
        validate(customer);
        if (customerRepository.existsByPhone(customer.getPhone())) {
            throw new BadRequestException("Số điện thoại đã tồn tại");
        }
        customer.setId(null);
        customer.setLoyaltyPoints(0);
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng id = " + id));
    }

    @Override
    @Transactional
    public Customer update(Long id, Customer data) {
        validate(data);
        Customer existing = getById(id);
        if (customerRepository.existsByPhoneAndIdNot(data.getPhone(), id)) {
            throw new BadRequestException("Số điện thoại đã tồn tại");
        }
        existing.setFullName(data.getFullName());
        existing.setPhone(data.getPhone());
        existing.setEmail(data.getEmail());
        return customerRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        customerRepository.delete(getById(id));
    }

    private void validate(Customer customer) {
        if (customer == null || customer.getFullName() == null || customer.getFullName().isBlank()) {
            throw new BadRequestException("Họ tên khách hàng không được để trống");
        }
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }
    }
}
