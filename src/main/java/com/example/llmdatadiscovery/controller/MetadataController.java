package com.example.llmdatadiscovery.controller;

import com.example.llmdatadiscovery.entity.MetadataRecord;
import com.example.llmdatadiscovery.repository.ColumnRecordRepository;
import com.example.llmdatadiscovery.repository.MetadataRecordRepository;
import com.example.llmdatadiscovery.service.DatabaseMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/metadata")
public class MetadataController {

    private final MetadataRecordRepository metadataRepository;
    private final ColumnRecordRepository columnRepository;
    private final DatabaseMetadataService databaseMetadataService;

    public MetadataController(MetadataRecordRepository metadataRepository, ColumnRecordRepository columnRepository, DatabaseMetadataService databaseMetadataService) {
        this.metadataRepository = metadataRepository;
        this.columnRepository = columnRepository;
        this.databaseMetadataService = databaseMetadataService;
    }

    @PostMapping("/db/metadata")
    public ResponseEntity<?> extractMetadata(@RequestBody com.example.llmdatadiscovery.dto.DBConnectionInput dbInput) {
        try {
            Map<String, Object> result = databaseMetadataService.extractAndSaveMetadata(dbInput);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("detail", e.getMessage()));
        }
    }

    // 3. Metadata List Service
    @GetMapping
    public List<Map<String, Object>> listMetadata() {
        return metadataRepository.findAll().stream().map(record -> {
            long tableCount = columnRepository.countByMetadataRecord_MetadataId(record.getMetadataId());
            return Map.<String, Object>of(
                    "metadata_id", record.getMetadataId(),
                    "database_name", record.getDbName(),
                    "creation_date", record.getCreationDate(),
                    "table_count", tableCount
            );
        }).collect(Collectors.toList());
    }

    @GetMapping("/{metadataId}")
    public ResponseEntity<?> getMetadataDetail(@PathVariable String metadataId) {
        if (!metadataRepository.existsById(metadataId)) {
            return ResponseEntity.status(404).body(Map.of("detail", "Metadata bulunamadı."));
        }

        List<com.example.llmdatadiscovery.entity.ColumnRecord> columns = columnRepository.findAll();

        // Sadece bu metadata_id'ye ait olanları filtrele ve tablo ismine göre grupla
        Map<String, List<Map<String, String>>> tablesDict = columns.stream()
                .filter(c -> c.getMetadataRecord().getMetadataId().equals(metadataId))
                .collect(Collectors.groupingBy(
                        com.example.llmdatadiscovery.entity.ColumnRecord::getTableName,
                        Collectors.mapping(c -> Map.of(
                                "column_id", c.getColumnId(),
                                "column_name", c.getColumnName(),
                                "data_type", c.getDataType()
                        ), Collectors.toList())
                ));

        List<Map<String, Object>> tablesInfo = tablesDict.entrySet().stream()
                .map(entry -> Map.of(
                        "table_name", entry.getKey(),
                        "columns", entry.getValue()
                )).toList();

        return ResponseEntity.ok(Map.of(
                "metadata_id", metadataId,
                "tables", tablesInfo
        ));
    }

    // 5. Metadata Delete Service
    @DeleteMapping("/{metadataId}")
    public ResponseEntity<Map<String, String>> deleteMetadata(@PathVariable String metadataId) {
        if (!metadataRepository.existsById(metadataId)) {
            return ResponseEntity.status(404).body(Map.of("detail", "Metadata bulunamadı."));
        }

        columnRepository.deleteByMetadataRecord_MetadataId(metadataId);
        metadataRepository.deleteById(metadataId);

        return ResponseEntity.ok(Map.of(
                "deletion_status", "Success",
                "message", "Metadata " + metadataId + " başarıyla silindi."
        ));
    }
}