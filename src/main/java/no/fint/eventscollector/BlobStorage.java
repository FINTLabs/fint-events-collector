package no.fint.eventscollector;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Service
@Slf4j
public class BlobStorage {

    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient blobContainerClient;
    private final ObjectMapper objectMapper;

    public BlobStorage(
            @Value("${fint.audit.blob.connection-string}") String connectionString,
            @Value("${fint.audit.blob.container-name}") String containerName,
            ObjectMapper objectMapper) {
        blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        this.objectMapper = objectMapper;
        if (!blobContainerClient.exists()) {
            blobContainerClient.create();
        }
        log.info("Connected to {}", blobServiceClient.getAccountName());
    }

    public void storeEvents(String orgId, List<AuditEvent> events) throws IOException {
        LocalDateTime dateTime = LocalDateTime.now();
        String systemId = String.format("events-%s-%TF-%<TT.json.gz", orgId, dateTime);
        BlobClient blobClient = blobContainerClient.getBlobClient(systemId);
        try (GZIPOutputStream outputStream = new GZIPOutputStream(blobClient.getBlockBlobClient().getBlobOutputStream())) {
            objectMapper.writeValue(outputStream, events);
        }
        blobClient.setMetadata(ImmutableMap.<String, String>builder()
                .put("date", dateTime.toString())
                .put("orgId", orgId)
                .build());
    }
}
