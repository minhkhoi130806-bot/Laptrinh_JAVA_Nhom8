package com.restaurant.backend.controller;

import com.restaurant.backend.dto.EmployeeRequest;
import com.restaurant.backend.dto.EmployeeResponse;
import com.restaurant.backend.dto.ListScope;
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

    // GET /api/employees  |  ?scope=TRASH  |  ?scope=ALL
    @GetMapping
    public List<EmployeeResponse> getAllEmployees(@RequestParam(defaultValue = "ACTIVE") ListScope scope) {
        return employeeService.getAll(scope).stream().map(employeeMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployee(@PathVariable Long id) {
        return employeeMapper.toResponse(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    public EmployeeResponse updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        return employeeMapper.toResponse(employeeService.update(id, employeeMapper.toEntity(request)));
    }

    // Chuyển vào thùng rác
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
    }

    // Khôi phục từ thùng rác
    @PostMapping("/{id}/restore")
    public EmployeeResponse restoreEmployee(@PathVariable Long id) {
        return employeeMapper.toResponse(employeeService.restore(id));
    }

    // Xóa vĩnh viễn (chỉ khi đang ở thùng rác)
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purgeEmployee(@PathVariable Long id) {
        employeeService.purge(id);
    }
}
