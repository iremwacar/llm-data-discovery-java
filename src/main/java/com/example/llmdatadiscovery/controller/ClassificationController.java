package com.example.llmdatadiscovery.controller;

import com.example.llmdatadiscovery.dto.ClassifyRequest;
import com.example.llmdatadiscovery.service.ClassificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/classify")
public class ClassificationController {

    private final ClassificationService classificationService;

    public ClassificationController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping
    public ResponseEntity<?> classifyData(@RequestBody ClassifyRequest request) {
        try {
            Map<String, Object> result = classificationService.classifyMetadata(
                    request.getMetadataId(),
                    request.getSampleLimit()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("detail", e.getMessage()));
        }
    }
}