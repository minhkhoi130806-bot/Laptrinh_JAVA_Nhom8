package com.restaurant.backend.controller;

import com.restaurant.backend.dto.OperationRequest;
import com.restaurant.backend.dto.OperationResponse;
import com.restaurant.backend.service.OperationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class OperationController {
    private final OperationService service;
    public OperationController(OperationService service) { this.service = service; }

    @GetMapping("/operations/{type}")
    public List<OperationResponse> list(@PathVariable String type) { return service.list(type); }
    @PostMapping("/operations/{type}") @ResponseStatus(HttpStatus.CREATED)
    public OperationResponse create(@PathVariable String type, @RequestBody OperationRequest request) { return service.create(type, request); }
    @PutMapping("/operations/{type}/{id}")
    public OperationResponse update(@PathVariable String type, @PathVariable Long id, @RequestBody OperationRequest request) { return service.update(type, id, request); }
    @DeleteMapping("/operations/{type}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String type, @PathVariable Long id, @RequestParam Long version) { service.delete(type, id, version); }
    @GetMapping("/settings")
    public OperationResponse settings() { return service.settings(); }
    @PutMapping("/settings")
    public OperationResponse saveSettings(@RequestBody OperationRequest request) { return service.saveSettings(request); }
    @DeleteMapping("/settings") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetSettings(@RequestParam Long version) { service.resetSettings(version); }
}
