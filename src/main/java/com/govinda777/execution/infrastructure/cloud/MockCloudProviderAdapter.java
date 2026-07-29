package com.govinda777.execution.infrastructure.cloud;

import com.govinda777.execution.business.gateway.CloudProviderGateway;
import com.govinda777.execution.business.model.CloudAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockCloudProviderAdapter implements CloudProviderGateway {

    private static final Logger log = LoggerFactory.getLogger(MockCloudProviderAdapter.class);

    @Override
    public void provisionAccount(CloudAccount account) throws Exception {
        log.info("Simulating provisioning of cloud account: {} with provider: {}", account.getName(), account.getProvider());
        
        // Simular latência de rede / API
        Thread.sleep(1000);

        if (account.getName().equalsIgnoreCase("fail-me")) {
            throw new RuntimeException("Simulated cloud provisioning failure");
        }

        log.info("Successfully provisioned account: {} on provider: {}", account.getName(), account.getProvider());
    }

    @Override
    public void linkBilling(CloudAccount account, CloudAccount seedAccount) throws Exception {
        log.info("Linking billing of account: {} to seed account: {} (ID: {})", 
                 account.getName(), seedAccount.getName(), seedAccount.getId());
        
        // Simular latência de rede / API
        Thread.sleep(500);

        log.info("Successfully linked billing for account: {}", account.getName());
    }
}
