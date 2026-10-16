package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.CustomerRequest;
import com.restaurant.backend.dto.CustomerResponse;
import com.restaurant.backend.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        if (request == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        // null = không đổi điểm (service tự xử lý)
        customer.setLoyaltyPoints(request.loyaltyPoints());
        return customer;
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getLoyaltyPoints(),
                customer.getDeleted(),
                customer.getDeletedAt()
        );
    }
}
