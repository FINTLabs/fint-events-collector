package no.fint.eventscollector;

import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class Collector implements ApplicationRunner {
    private final EventsService eventsService;
    private final BlobStorage blobStorage;

    public Collector(EventsService eventsService, BlobStorage blobStorage) {
        this.eventsService = eventsService;
        this.blobStorage = blobStorage;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (String orgId : args.getNonOptionArgs()) {
            List<AuditEvent> events = eventsService.getAuditEvents(orgId, "24h");
            blobStorage.storeEvents(orgId, events);
            log.info("Stored {} events for {}", events.size(), orgId);
        }
    }
}
