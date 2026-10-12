package com.example.llmdatadiscovery.dto;

import lombok.Data;

@Data
public class ClassifyRequest {
    private String metadataId;
    private Integer sampleLimit = 10;
}