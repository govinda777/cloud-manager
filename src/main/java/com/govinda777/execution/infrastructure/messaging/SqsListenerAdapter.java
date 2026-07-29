package com.govinda777.execution.infrastructure.messaging;

import com.govinda777.execution.business.logic.ProcessAccountProvisioningUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SqsListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SqsListenerAdapter.class);
    private final ProcessAccountProvisioningUseCase processAccountProvisioningUseCase;

    public SqsListenerAdapter(ProcessAccountProvisioningUseCase processAccountProvisioningUseCase) {
        this.processAccountProvisioningUseCase = processAccountProvisioningUseCase;
    }

    @SqsListener("${aws.sqs.queue-name:account-provisioning-queue}")
    public void listen(String message) {
        log.info("Received provisioning request for account ID: {}", message);
        try {
            Long accountId = Long.parseLong(message);
            processAccountProvisioningUseCase.execute(accountId);
        } catch (NumberFormatException e) {
            log.error("Invalid account ID format in SQS message: {}", message, e);
        } catch (Exception e) {
            log.error("Error processing provisioning event for account ID: {}", message, e);
        }
    }
}
