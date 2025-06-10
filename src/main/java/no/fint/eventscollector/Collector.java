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
    private final boolean restartProcessor;

    public Collector(
            EventsService eventsService,
            BlobStorage blobStorage,
            @Value("${fint.audit.orgids}") String[] orgids,
            @Value("${fint.audit.collect}") Duration collectDuration,
            @Value("${fint.audit.retain}") Duration retainDuration,
            @Value("${fint.audit.restart:false}") boolean restartProcessor) {
        this.eventsService = eventsService;
        this.blobStorage = blobStorage;
        this.orgids = orgids;
        this.collectDuration = collectDuration;
        this.retainDuration = retainDuration;
        this.restartProcessor = restartProcessor;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Deleting events older than {} ...", collectDuration);
        eventsService.deleteAuditEvents(collectDuration);

        for (String orgId : orgids) {
            log.info("Start collect {} ...", orgId);

            Flux<AuditEvent> events = eventsService
                    .getAuditEvents(orgId, collectDuration)
                    .map(this::maskQuery);

            long size = blobStorage.storeEvents(orgId, events);
            log.info("Stored {} events for {}", size, orgId);
        }

        eventsService.deleteAuditEvents(retainDuration);
        Long size = eventsService.getRepositorySize();
        log.info("Repository size is {}", size);
        if (restartProcessor) {
            log.info("Restarting processor..");
            eventsService.stopProcessor();
            eventsService.startProcessor();
            log.info("Processor restarted.");
        }
    }

    private AuditEvent maskQuery(AuditEvent auditEvent) {
        var event = auditEvent.getEvent();
        if (event != null) {
            event.setMessage(event.getFilteredMessage());
            if (event.getRequest() != null) {
                event.setQuery(event.getRequest().getFilteredQuery());
            }
        }
        return auditEvent;
    }

}
