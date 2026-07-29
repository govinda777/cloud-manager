package com.govinda777.execution.business.logic;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.gateway.QueueGateway;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateAccountUseCaseTest {

    private AccountRepositoryGateway accountRepository;
    private QueueGateway queueGateway;
    private CreateAccountUseCase createAccountUseCase;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepositoryGateway.class);
        queueGateway = mock(QueueGateway.class);
        createAccountUseCase = new CreateAccountUseCase(accountRepository, queueGateway);
    }

    @Test
    void shouldCreateAccountSuccessfully() {
        CloudAccount inputAccount = new CloudAccount();
        inputAccount.setName("Test Account");
        inputAccount.setEmail("test@domain.com");
        inputAccount.setProvider("AWS");
        inputAccount.setCostCenter("CC-101");

        CloudAccount savedAccount = new CloudAccount();
        savedAccount.setId(1L);
        savedAccount.setName("Test Account");
        savedAccount.setEmail("test@domain.com");
        savedAccount.setProvider("AWS");
        savedAccount.setState(AccountState.CREATED);
        savedAccount.setCostCenter("CC-101");

        when(accountRepository.save(any(CloudAccount.class))).thenReturn(savedAccount);

        CloudAccount result = createAccountUseCase.execute(inputAccount);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(AccountState.CREATED, result.getState());
        verify(accountRepository, times(1)).save(any(CloudAccount.class));
        verify(queueGateway, times(1)).publishProvisioningEvent(1L);
    }

    @Test
    void shouldThrowExceptionWhenProviderIsInvalid() {
        CloudAccount inputAccount = new CloudAccount();
        inputAccount.setName("Test Account");
        inputAccount.setEmail("test@domain.com");
        inputAccount.setProvider("AZURE"); // Invalid
        inputAccount.setCostCenter("CC-101");

        assertThrows(IllegalArgumentException.class, () -> createAccountUseCase.execute(inputAccount));
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(queueGateway);
    }

    @Test
    void shouldThrowExceptionWhenCostCenterIsMissing() {
        CloudAccount inputAccount = new CloudAccount();
        inputAccount.setName("Test Account");
        inputAccount.setEmail("test@domain.com");
        inputAccount.setProvider("AWS");
        inputAccount.setCostCenter(""); // Invalid

        assertThrows(IllegalArgumentException.class, () -> createAccountUseCase.execute(inputAccount));
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(queueGateway);
    }
}
