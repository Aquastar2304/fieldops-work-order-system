package com.fieldops.dto;

public record AssetDto(
        Long id, String tag, String name, String manufacturer, String model, Long siteId, String siteName) {}
