package no.fint.eventscollector;

import no.fint.audit.model.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class EventsService {
    private final WebClient webClient;

    public EventsService(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<AuditEvent> getAuditEvents(String orgId, String period) {
        return webClient
                .get()
                .uri("/api/{orgId}?period={period}&limit={limit}", orgId, period, 100_000_000L)
                .retrieve()
                .bodyToFlux(AuditEvent.class)
                .collectList()
                .block();
    }
}
