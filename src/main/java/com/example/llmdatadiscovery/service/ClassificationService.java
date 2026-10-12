package com.example.llmdatadiscovery.service;

import com.example.llmdatadiscovery.entity.ColumnRecord;
import com.example.llmdatadiscovery.entity.MetadataRecord;
import com.example.llmdatadiscovery.repository.ColumnRecordRepository;
import com.example.llmdatadiscovery.repository.MetadataRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.util.*;

@Service
public class ClassificationService {

    private final MetadataRecordRepository metadataRepository;
    private final ColumnRecordRepository columnRepository;
    private final EncryptionService encryptionService;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    public ClassificationService(MetadataRecordRepository metadataRepository,
                                 ColumnRecordRepository columnRepository,
                                 EncryptionService encryptionService) {
        this.metadataRepository = metadataRepository;
        this.columnRepository = columnRepository;
        this.encryptionService = encryptionService;
    }



    public Map<String, Object> classifyMetadata(String metadataId, Integer sampleLimit) throws Exception {
        // Eğer istekte limit belirtilmemişse varsayılan olarak 10 al
        int limit = (sampleLimit != null && sampleLimit > 0) ? sampleLimit : 10;

        MetadataRecord metadataRecord = metadataRepository.findById(metadataId)
                .orElseThrow(() -> new RuntimeException("Metadata bulunamadı: " + metadataId));

        String decryptedPassword = encryptionService.decryptData(metadataRecord.getEncryptedPassword());
        String dbUrl = String.format("jdbc:postgresql://%s:%d/%s",
                metadataRecord.getHost(), metadataRecord.getPort(), metadataRecord.getDbName());

        List<ColumnRecord> columns = columnRepository.findAll().stream()
                .filter(c -> c.getMetadataRecord().getMetadataId().equals(metadataId))
                .toList();

        Map<String, List<Map<String, Object>>> schemaWithSamples = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(dbUrl, metadataRecord.getUsername(), decryptedPassword)) {
            for (ColumnRecord col : columns) {
                List<String> samples = new ArrayList<>();
                // LIMIT kısmını dinamik 'limit' değişkeni yaptık
                String query = String.format("SELECT \"%s\" FROM \"%s\" WHERE \"%s\" IS NOT NULL LIMIT %d",
                        col.getColumnName(), col.getTableName(), col.getColumnName(), limit);

                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        samples.add(rs.getString(1));
                    }
                } catch (Exception e) {
                    // Örnek veri çekilemezse boş devam et
                }

                Map<String, Object> colData = new HashMap<>();
                colData.put("column_name", col.getColumnName());
                colData.put("data_type", col.getDataType());
                colData.put("sample_values", samples);

                schemaWithSamples.computeIfAbsent(col.getTableName(), k -> new ArrayList<>()).add(colData);
            }
        }

        return callOpenAiClassification(metadataId, schemaWithSamples);
    }

    private Map<String, Object> callOpenAiClassification(String metadataId, Map<String, List<Map<String, Object>>> schema) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com/v1/chat/completions";

        String prompt = "Sen bir Veri Güvenliği ve PII (Kişisel Veri) tespit uzmanısın.\n" +
                "Sana verilen veritabanı şeması ve örnek değerleri inceleyerek her kolon için PII türünü ve hassasiyet seviyesini belirle.\n\n" +
                "Şema Verisi:\n" + schema.toString() + "\n\n" +
                "Lütfen YALNIZCA geçerli bir JSON yanıtı döndür. Format tam olarak şöyle olmalıdır:\n" +
                "{\n" +
                "  \"classification_results\": [\n" +
                "    {\n" +
                "      \"table_name\": \"tablo_adı\",\n" +
                "      \"column_name\": \"kolon_adı\",\n" +
                "      \"pii_type\": \"EMAIL | PHONE | NATIONAL_ID | NAME | ADDRESS | NONE | OTHER\",\n" +
                "      \"sensitivity\": \"HIGH | MEDIUM | LOW | NONE\",\n" +
                "      \"confidence_score\": 0.95\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "Sen JSON formatında çıktı veren bir AI asistanısın."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map body = response.getBody();
            if (body != null && body.containsKey("choices")) {
                List choices = (List) body.get("choices");
                Map firstChoice = (Map) choices.get(0);
                Map message = (Map) firstChoice.get("message");
                String content = (String) message.get("content");

                // --- DÜZELTME BURADA: Metin olan JSON'ı gerçek bir JSON Objesine/Map'e dönüştürüyoruz ---
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object parsedContent = mapper.readValue(content, Object.class);

                return Map.of(
                        "metadata_id", metadataId,
                        "status", "COMPLETED",
                        "llm_analysis", parsedContent
                );
            }
        } catch (Exception e) {
            return Map.of("error", "LLM Analizi sırasında hata oluştu: " + e.getMessage());
        }

        return Map.of("error", "LLM yanıtı alınamadı.");
    }
}