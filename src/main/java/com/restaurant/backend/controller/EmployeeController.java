package com.restaurant.backend.controller;

import com.restaurant.backend.dto.EmployeeRequest;
import com.restaurant.backend.dto.EmployeeResponse;
import com.restaurant.backend.mapper.EmployeeMapper;
import com.restaurant.backend.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    public EmployeeController(EmployeeService employeeService, EmployeeMapper employeeMapper) {
        this.employeeService = employeeService;
        this.employeeMapper = employeeMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse addEmployee(@RequestBody EmployeeRequest request) {
        return employeeMapper.toResponse(employeeService.create(employeeMapper.toEntity(request)));
    }

    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.getAll().stream().map(employeeMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployee(@PathVariable Long id) {
        return employeeMapper.toResponse(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    public EmployeeResponse updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        return employeeMapper.toResponse(employeeService.update(id, employeeMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
    }
}
