package com.govinda777.execution.business.logic;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.gateway.CloudProviderGateway;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessAccountProvisioningUseCaseTest {

    private AccountRepositoryGateway accountRepository;
    private CloudProviderGateway cloudProviderGateway;
    private ProcessAccountProvisioningUseCase processUseCase;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepositoryGateway.class);
        cloudProviderGateway = mock(CloudProviderGateway.class);
        processUseCase = new ProcessAccountProvisioningUseCase(accountRepository, cloudProviderGateway);
    }

    @Test
    void shouldProcessProvisioningSuccessfully() throws Exception {
        Long accountId = 1L;
        CloudAccount childAccount = new CloudAccount();
        childAccount.setId(accountId);
        childAccount.setName("Child-1");
        childAccount.setEmail("child1@gcp.com");
        childAccount.setProvider("GCP");
        childAccount.setState(AccountState.CREATED);

        CloudAccount seedAccount = new CloudAccount();
        seedAccount.setId(100L);
        seedAccount.setName("GCP-Billing-Seed");
        seedAccount.setProvider("GCP");
        seedAccount.setState(AccountState.ACTIVE);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(childAccount));
        when(accountRepository.findSeedAccount("GCP")).thenReturn(Optional.of(seedAccount));

        processUseCase.execute(accountId);

        assertEquals(AccountState.ACTIVE, childAccount.getState());
        assertEquals(100L, childAccount.getSeedAccountId());
        verify(cloudProviderGateway, times(1)).provisionAccount(childAccount);
        verify(cloudProviderGateway, times(1)).linkBilling(childAccount, seedAccount);
        verify(accountRepository, atLeastOnce()).save(childAccount);
    }

    @Test
    void shouldTransitionToFailedWhenProvisioningFails() throws Exception {
        Long accountId = 1L;
        CloudAccount childAccount = new CloudAccount();
        childAccount.setId(accountId);
        childAccount.setName("Child-1");
        childAccount.setEmail("child1@gcp.com");
        childAccount.setProvider("GCP");
        childAccount.setState(AccountState.CREATED);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(childAccount));
        doThrow(new RuntimeException("Cloud provider timeout")).when(cloudProviderGateway).provisionAccount(any());

        processUseCase.execute(accountId);

        assertEquals(AccountState.FAILED, childAccount.getState());
        assertEquals("Cloud provider timeout", childAccount.getErrorMessage());
        verify(accountRepository, atLeastOnce()).save(childAccount);
    }
}
