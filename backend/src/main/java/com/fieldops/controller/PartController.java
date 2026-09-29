package com.fieldops.controller;

import com.fieldops.dto.PartDto;
import com.fieldops.dto.PartRequest;
import com.fieldops.service.PartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/parts")
@Tag(name = "Parts")
public class PartController {

    private final PartService service;

    public PartController(PartService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all parts with their current stock level")
    public List<PartDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single part")
    public PartDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a part")
    public ResponseEntity<PartDto> create(@Valid @RequestBody PartRequest request) {
        PartDto created = service.create(request);
        return ResponseEntity.created(URI.create("/api/parts/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a part")
    public PartDto update(@PathVariable Long id, @Valid @RequestBody PartRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a part")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
