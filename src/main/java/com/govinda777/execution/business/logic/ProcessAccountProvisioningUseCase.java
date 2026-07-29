package com.govinda777.execution.business.logic;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.gateway.CloudProviderGateway;
import com.govinda777.execution.business.model.CloudAccount;

import java.util.Optional;

public class ProcessAccountProvisioningUseCase {
    private final AccountRepositoryGateway accountRepository;
    private final CloudProviderGateway cloudProviderGateway;

    public ProcessAccountProvisioningUseCase(AccountRepositoryGateway accountRepository, 
                                             CloudProviderGateway cloudProviderGateway) {
        this.accountRepository = accountRepository;
        this.cloudProviderGateway = cloudProviderGateway;
    }

    public void execute(Long accountId) {
        Optional<CloudAccount> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return;
        }

        CloudAccount account = accountOpt.get();

        try {
            // 1. Iniciar Provisionamento
            account.startProvisioning();
            accountRepository.save(account);

            cloudProviderGateway.provisionAccount(account);

            // 2. Vincular Conta de Faturamento (Billing/Seed Account)
            Optional<CloudAccount> seedAccountOpt = accountRepository.findSeedAccount(account.getProvider());
            if (seedAccountOpt.isEmpty()) {
                throw new IllegalStateException("Seed/Billing account not found for provider " + account.getProvider());
            }

            CloudAccount seedAccount = seedAccountOpt.get();
            cloudProviderGateway.linkBilling(account, seedAccount);

            account.linkBilling(seedAccount.getId());
            accountRepository.save(account);

            // 3. Ativação Final
            account.activate();
            accountRepository.save(account);

        } catch (Exception e) {
            account.fail(e.getMessage());
            accountRepository.save(account);
        }
    }
}
