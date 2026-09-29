package com.fieldops.controller;

import com.fieldops.dto.TechnicianDto;
import com.fieldops.dto.TechnicianRequest;
import com.fieldops.service.TechnicianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/technicians")
@Tag(name = "Technicians")
public class TechnicianController {

    private final TechnicianService service;

    public TechnicianController(TechnicianService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all technicians")
    public List<TechnicianDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single technician")
    public TechnicianDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a technician")
    public ResponseEntity<TechnicianDto> create(@Valid @RequestBody TechnicianRequest request) {
        TechnicianDto created = service.create(request);
        return ResponseEntity.created(URI.create("/api/technicians/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a technician")
    public TechnicianDto update(@PathVariable Long id, @Valid @RequestBody TechnicianRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a technician")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
