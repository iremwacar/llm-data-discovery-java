package com.example.llmdatadiscovery.repository;

import com.example.llmdatadiscovery.entity.MetadataRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadataRecordRepository extends JpaRepository<MetadataRecord, String> {
}