package com.govinda777.execution.business.gateway;

import com.govinda777.execution.business.model.CloudAccount;

public interface CloudProviderGateway {
    void provisionAccount(CloudAccount account) throws Exception;
    void linkBilling(CloudAccount account, CloudAccount seedAccount) throws Exception;
}
