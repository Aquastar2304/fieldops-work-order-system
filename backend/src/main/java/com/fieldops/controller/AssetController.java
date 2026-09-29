package com.fieldops.controller;

import com.fieldops.dto.AssetDto;
import com.fieldops.dto.AssetRequest;
import com.fieldops.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets")
public class AssetController {

    private final AssetService service;

    public AssetController(AssetService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List assets, optionally filtered by site")
    public List<AssetDto> list(@RequestParam(required = false) Long siteId) {
        return service.findAll(siteId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single asset")
    public AssetDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create an asset")
    public ResponseEntity<AssetDto> create(@Valid @RequestBody AssetRequest request) {
        AssetDto created = service.create(request);
        return ResponseEntity.created(URI.create("/api/assets/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an asset")
    public AssetDto update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an asset")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
