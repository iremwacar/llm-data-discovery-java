package com.example.llmdatadiscovery.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "column_records")
public class ColumnRecord {

    @Id
    private String columnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_id", nullable = false)
    private MetadataRecord metadataRecord;

    private String tableName;
    private String columnName;
    private String dataType;
}