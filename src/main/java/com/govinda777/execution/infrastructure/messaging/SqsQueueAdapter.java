package com.govinda777.execution.infrastructure.messaging;

import com.govinda777.execution.business.gateway.QueueGateway;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;

public class SqsQueueAdapter implements QueueGateway {

    private final SqsTemplate sqsTemplate;

    @Value("${aws.sqs.queue-name:account-provisioning-queue}")
    private String queueName;

    public SqsQueueAdapter(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void publishProvisioningEvent(Long accountId) {
        sqsTemplate.send(queueName, accountId.toString());
    }
}
