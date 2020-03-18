package no.fint.eventscollector;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Consumer;
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

    private Consumer<AuditEvent> writer(JsonGenerator generator) {
        return it -> {
            try {
                objectMapper.writeValue(generator, it);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public long storeEvents(String orgId, Flux<AuditEvent> events) throws IOException {
        long count;
        LocalDateTime dateTime = LocalDateTime.now();
        String systemId = String.format("events-%s-%TF-%<TT.json.gz", orgId, dateTime);
        BlobClient blobClient = blobContainerClient.getBlobClient(systemId);
        try (JsonGenerator generator = objectMapper
                .getFactory()
                .createGenerator(
                        new GZIPOutputStream(
                                blobClient.getBlockBlobClient().getBlobOutputStream()
                        )
                )
        ) {
            generator.writeStartArray();
            count = events.toStream()
                    .peek(writer(generator))
                    .count();
            generator.writeEndArray();
        }
        blobClient.setMetadata(ImmutableMap.<String, String>builder()
                .put("date", dateTime.toString())
                .put("orgId", orgId)
                .put("count", String.valueOf(count))
                .build());
        return count;
    }
}
