package com.example.llmdatadiscovery.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data // Lombok: Otomatik olarak Getter, Setter ve toString metodlarını üretir
@Entity
@Table(name = "metadata_records")
public class MetadataRecord {

    @Id
    private String metadataId;

    private String dbName;
    private String host;
    private Integer port;
    private String username;
    private String encryptedPassword;
    private String creationDate;
}