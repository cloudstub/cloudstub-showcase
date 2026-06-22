package io.cloudstub.showcase.ingestion;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the ingestion queue on a fixed interval in a running application. Disabled under the {@code
 * test} profile, where the test drives {@link IngestionProcessor#processPending()} directly for
 * deterministic timing.
 */
@Component
@Profile("!test")
public class IngestionScheduler {

    private final IngestionProcessor processor;

    public IngestionScheduler(IngestionProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${showcase.ingestion.poll-interval-ms:2000}")
    public void poll() {
        processor.processPending();
    }
}
