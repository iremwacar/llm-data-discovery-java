package com.example.llmdatadiscovery.repository;

import com.example.llmdatadiscovery.entity.ColumnRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ColumnRecordRepository extends JpaRepository<ColumnRecord, String> {

    // Python'daki db.query(ColumnRecord).filter(...).delete() işleminin karşılığı
    @Transactional
    void deleteByMetadataRecord_MetadataId(String metadataId);

    // Belirli bir metadata'ya ait tüm kolonları saymak için (Metadata List Service için)
    long countByMetadataRecord_MetadataId(String metadataId);
}