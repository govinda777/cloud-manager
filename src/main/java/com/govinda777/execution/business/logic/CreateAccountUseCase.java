package com.govinda777.execution.business.logic;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.gateway.QueueGateway;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;

import java.time.LocalDateTime;
import java.util.Optional;

public class CreateAccountUseCase {
    private final AccountRepositoryGateway accountRepository;
    private final QueueGateway queueGateway;

    public CreateAccountUseCase(AccountRepositoryGateway accountRepository, QueueGateway queueGateway) {
        this.accountRepository = accountRepository;
        this.queueGateway = queueGateway;
    }

    public CloudAccount execute(CloudAccount account) {
        // Enforce validations
        if (account.getProvider() == null || (!account.getProvider().equals("AWS") && !account.getProvider().equals("GCP"))) {
            throw new IllegalArgumentException("Provider must be AWS or GCP");
        }
        if (account.getCostCenter() == null || account.getCostCenter().isBlank()) {
            throw new IllegalArgumentException("Cost center is required");
        }

        // Try booking from the Account Pool
        Optional<CloudAccount> poolAccountOpt = accountRepository.findAvailablePoolAccount(account.getProvider());
        if (poolAccountOpt.isPresent()) {
            CloudAccount poolAccount = poolAccountOpt.get();
            poolAccount.book(account.getName(), account.getEmail(), account.getCostCenter());
            poolAccount.activate();
            CloudAccount saved = accountRepository.save(poolAccount);

            // Trigger background replenishment to maintain the pool size
            triggerPoolReplenish(account.getProvider());

            return saved;
        }

        // Fallback: Create and provision from scratch if the pool is empty
        account.setState(AccountState.CREATED);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        CloudAccount savedAccount = accountRepository.save(account);

        // Dispara o fluxo assíncrono via fila
        queueGateway.publishProvisioningEvent(savedAccount.getId());

        return savedAccount;
    }

    private void triggerPoolReplenish(String provider) {
        CloudAccount newPoolAccount = new CloudAccount();
        newPoolAccount.setName(provider + "-Pool-Account-" + System.currentTimeMillis());
        newPoolAccount.setEmail("pool-billing-" + provider.toLowerCase() + "@corporate.com");
        newPoolAccount.setProvider(provider);
        newPoolAccount.setCostCenter("CC-POOL-PREPROVISION");
        newPoolAccount.setState(AccountState.CREATED);
        newPoolAccount.setCreatedAt(LocalDateTime.now());
        newPoolAccount.setUpdatedAt(LocalDateTime.now());
        
        CloudAccount saved = accountRepository.save(newPoolAccount);
        queueGateway.publishProvisioningEvent(saved.getId());
    }
}
