package com.example.llmdatadiscovery.service;

import com.example.llmdatadiscovery.dto.DBConnectionInput;
import com.example.llmdatadiscovery.entity.ColumnRecord;
import com.example.llmdatadiscovery.entity.MetadataRecord;
import com.example.llmdatadiscovery.repository.ColumnRecordRepository;
import com.example.llmdatadiscovery.repository.MetadataRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.*;

@Service
public class DatabaseMetadataService {

    private final MetadataRecordRepository metadataRepository;
    private final ColumnRecordRepository columnRepository;
    private final EncryptionService encryptionService;

    public DatabaseMetadataService(MetadataRecordRepository metadataRepository,
                                   ColumnRecordRepository columnRepository,
                                   EncryptionService encryptionService) {
        this.metadataRepository = metadataRepository;
        this.columnRepository = columnRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public Map<String, Object> extractAndSaveMetadata(DBConnectionInput input) throws Exception {
        // PostgreSQL bağlantı URL'sini oluştur
        String dbUrl = String.format("jdbc:postgresql://%s:%d/%s",
                input.getHost(), input.getPort(), input.getDatabase());

        String metadataId = UUID.randomUUID().toString();

        // Metadata kaydını oluştur ve parolayı şifrele
        MetadataRecord metadataRecord = new MetadataRecord();
        metadataRecord.setMetadataId(metadataId);
        metadataRecord.setDbName(input.getDatabase());
        metadataRecord.setHost(input.getHost());
        metadataRecord.setPort(input.getPort());
        metadataRecord.setUsername(input.getUsername());
        metadataRecord.setEncryptedPassword(encryptionService.encryptData(input.getPassword()));
        metadataRecord.setCreationDate(Instant.now().toString());

        metadataRepository.save(metadataRecord);

        List<Map<String, Object>> tablesInfo = new ArrayList<>();

        // JDBC ile hedef veritabanına bağlanıp şemayı oku
        try (Connection conn = DriverManager.getConnection(dbUrl, input.getUsername(), input.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();

            // "public" şemasındaki tabloları getir
            try (ResultSet tables = metaData.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    List<Map<String, String>> columnsInfo = new ArrayList<>();

                    // Tablonun kolonlarını getir
                    try (ResultSet columns = metaData.getColumns(null, "public", tableName, "%")) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            String dataType = columns.getString("TYPE_NAME");

                            String colId = UUID.randomUUID().toString();

                            ColumnRecord columnRecord = new ColumnRecord();
                            columnRecord.setColumnId(colId);
                            columnRecord.setMetadataRecord(metadataRecord);
                            columnRecord.setTableName(tableName);
                            columnRecord.setColumnName(columnName);
                            columnRecord.setDataType(dataType);

                            columnRepository.save(columnRecord);

                            columnsInfo.add(Map.of(
                                    "column_id", colId,
                                    "column_name", columnName,
                                    "data_type", dataType
                            ));
                        }
                    }
                    tablesInfo.add(Map.of(
                            "table_name", tableName,
                            "columns", columnsInfo
                    ));
                }
            }
        }

        return Map.of(
                "metadata_id", metadataId,
                "tables", tablesInfo
        );
    }
}