package no.fint.eventscollector;

import no.fintlabs.audit.model.AuditEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class EventsService {
    public static final String PROCESSOR = "/admin/processor";
    public static final String REPOSITORY = "/admin/repository";
    private final WebClient webClient;
    private final Duration timeout;

    public EventsService(
            WebClient webClient,
            @Value("${fint.audit.timeout}") Duration timeout
    ) {
        this.webClient = webClient;
        this.timeout = timeout;
    }

    public Flux<AuditEvent> getAuditEvents(String orgId, Duration period) {
        return webClient
                .get()
                .uri("/api/{orgId}?period={period}&limit={limit}", orgId, period, 100_000_000L)
                .retrieve()
                .bodyToFlux(AuditEvent.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(10))
                        .maxBackoff(Duration.ofMinutes(5))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()));
    }

    public Boolean getProcessorStatus() {
        return webClient
                .get()
                .uri(PROCESSOR)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block(timeout);
    }

    public void stopProcessor() {
        webClient
                .delete()
                .uri(PROCESSOR)
                .retrieve()
                .toBodilessEntity()
                .block(timeout);
    }

    public void startProcessor() {
        webClient
                .post()
                .uri(PROCESSOR)
                .retrieve()
                .toBodilessEntity()
                .block(timeout);
    }

    public Boolean deleteAuditEvents(Duration period) {
        return webClient
                .delete()
                .uri(REPOSITORY + "?since={period}", period)
                .retrieve()
                .toBodilessEntity()
                .map(response -> HttpStatus.valueOf(response.getStatusCode().value()).is2xxSuccessful())
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(10))
                        .maxBackoff(Duration.ofMinutes(5))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()))
                .block(timeout);
    }

    public Long getRepositorySize() {
        return webClient
                .get()
                .uri(REPOSITORY)
                .retrieve()
                .bodyToMono(Long.class)
                .block(timeout);
    }
}
