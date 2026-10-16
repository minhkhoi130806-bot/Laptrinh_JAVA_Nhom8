package com.restaurant.backend.controller;

import com.restaurant.backend.dto.CustomerRequest;
import com.restaurant.backend.dto.CustomerResponse;
import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.mapper.CustomerMapper;
import com.restaurant.backend.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public CustomerController(CustomerService customerService, CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse addCustomer(@RequestBody CustomerRequest request) {
        return customerMapper.toResponse(customerService.create(customerMapper.toEntity(request)));
    }

    // GET /api/customers  |  ?scope=TRASH (thùng rác)  |  ?scope=ALL (tra tên)
    @GetMapping
    public List<CustomerResponse> getAllCustomers(@RequestParam(defaultValue = "ACTIVE") ListScope scope) {
        return customerService.getAll(scope).stream().map(customerMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        return customerMapper.toResponse(customerService.getById(id));
    }

    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @RequestBody CustomerRequest request) {
        return customerMapper.toResponse(customerService.update(id, customerMapper.toEntity(request)));
    }

    // Chuyển vào thùng rác
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long id) {
        customerService.delete(id);
    }

    // Khôi phục từ thùng rác
    @PostMapping("/{id}/restore")
    public CustomerResponse restoreCustomer(@PathVariable Long id) {
        return customerMapper.toResponse(customerService.restore(id));
    }

    // Xóa vĩnh viễn (chỉ khi đang ở thùng rác)
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purgeCustomer(@PathVariable Long id) {
        customerService.purge(id);
    }
}
