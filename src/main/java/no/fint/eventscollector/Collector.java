package no.fint.eventscollector;

import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
@Slf4j
public class Collector implements ApplicationRunner {
    private final EventsService eventsService;
    private final BlobStorage blobStorage;
    private final String[] orgids;
    private final Duration collectDuration;
    private final Duration retainDuration;

    public Collector(
            EventsService eventsService,
            BlobStorage blobStorage,
            @Value("${fint.audit.orgids}") String[] orgids,
            @Value("${fint.audit.collect}") Duration collectDuration,
            @Value("${fint.audit.retain}") Duration retainDuration) {
        this.eventsService = eventsService;
        this.blobStorage = blobStorage;
        this.orgids = orgids;
        this.collectDuration = collectDuration;
        this.retainDuration = retainDuration;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Stopping processor..");
        eventsService.stopProcessor();
        for (String orgId : orgids) {
            log.info("Start collect {} ...", orgId);
            Flux<AuditEvent> events = eventsService.getAuditEvents(orgId, collectDuration);
            long size = blobStorage.storeEvents(orgId, events);
            log.info("Stored {} events for {}", size, orgId);
        }
        eventsService.deleteAuditEvents(retainDuration);
        Long size = eventsService.getRepositorySize();
        log.info("Repository size is {}", size);
        eventsService.startProcessor();
        log.info("Processor restarted.");
    }
}
