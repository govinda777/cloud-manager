package com.govinda777.execution.bdd;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.logic.CreateAccountUseCase;
import com.govinda777.execution.business.logic.ProcessAccountProvisioningUseCase;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import com.govinda777.execution.infrastructure.controller.AccountController;
import com.govinda777.execution.infrastructure.db.JpaAccountRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardAndErrorsSteps {

    @Autowired
    private CreateAccountUseCase createAccountUseCase;

    @Autowired
    private ProcessAccountProvisioningUseCase processAccountProvisioningUseCase;

    @Autowired
    private AccountRepositoryGateway repositoryGateway;

    @Autowired
    private JpaAccountRepository jpaAccountRepository;

    @Autowired
    private AccountController accountController;

    private CloudAccount createdAccount;
    private CloudAccount seedAccount;

    @Given("the database has seed accounts for {string}")
    public void ensureSeedAccount(String provider) {
        // Limpar banco para garantir previsibilidade de contadores nos testes
        jpaAccountRepository.deleteAll();

        seedAccount = new CloudAccount();
        seedAccount.setName(provider + "-Seed-Dashboard");
        seedAccount.setProvider(provider);
        seedAccount.setEmail("billing-seed-dash@" + provider.toLowerCase() + ".com");
        seedAccount.setCostCenter("CC-BILLING");
        seedAccount.setState(AccountState.ACTIVE);
        repositoryGateway.save(seedAccount);
    }

    @When("a new {string} account named {string} with email {string} and cost center {string} is created")
    public void createAccountForDashboard(String provider, String name, String email, String costCenter) {
        CloudAccount account = new CloudAccount();
        account.setName(name);
        account.setEmail(email);
        account.setProvider(provider);
        account.setCostCenter(costCenter);

        createdAccount = createAccountUseCase.execute(account);
        try {
            processAccountProvisioningUseCase.execute(createdAccount.getId());
        } catch (Exception e) {
            // Exceptions are caught inside the UseCase, but just in case
        }
    }

    @Then("the dashboard should show {int} total accounts")
    public void checkDashboardTotal(int expectedTotal) {
        ResponseEntity<AccountController.DashboardResponse> response = accountController.getDashboard();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(expectedTotal, response.getBody().getTotalAccounts());
    }

    @And("the dashboard active accounts should be {int}")
    public void checkDashboardActive(int expectedActive) {
        ResponseEntity<AccountController.DashboardResponse> response = accountController.getDashboard();
        assertNotNull(response.getBody());
        assertEquals(expectedActive, response.getBody().getActiveAccounts());
    }

    @Then("the account state should be {string}")
    public void checkAccountState(String expectedState) {
        CloudAccount fresh = repositoryGateway.findById(createdAccount.getId()).orElseThrow();
        assertEquals(expectedState, fresh.getState().name());
    }

    @And("the account detail should have error message {string}")
    public void checkErrorMessage(String expectedError) {
        CloudAccount fresh = repositoryGateway.findById(createdAccount.getId()).orElseThrow();
        assertEquals(expectedError, fresh.getErrorMessage());
    }
}
