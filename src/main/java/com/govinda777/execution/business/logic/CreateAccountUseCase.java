package com.govinda777.execution.business.logic;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.gateway.QueueGateway;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;

import java.time.LocalDateTime;

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

        account.setState(AccountState.CREATED);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        CloudAccount savedAccount = accountRepository.save(account);

        // Dispara o fluxo assíncrono via fila
        queueGateway.publishProvisioningEvent(savedAccount.getId());

        return savedAccount;
    }
}
