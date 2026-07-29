package com.govinda777.execution.business.gateway;

public interface QueueGateway {
    void publishProvisioningEvent(Long accountId);
}
