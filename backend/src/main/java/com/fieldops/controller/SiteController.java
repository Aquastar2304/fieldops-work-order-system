package com.fieldops.controller;

import com.fieldops.dto.SiteDto;
import com.fieldops.dto.SiteRequest;
import com.fieldops.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sites")
@Tag(name = "Sites")
public class SiteController {

    private final SiteService service;

    public SiteController(SiteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all sites")
    public List<SiteDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single site")
    public SiteDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a site")
    public ResponseEntity<SiteDto> create(@Valid @RequestBody SiteRequest request) {
        SiteDto created = service.create(request);
        return ResponseEntity.created(URI.create("/api/sites/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a site")
    public SiteDto update(@PathVariable Long id, @Valid @RequestBody SiteRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a site")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
