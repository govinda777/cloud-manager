package com.govinda777.execution.bdd;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.logic.CreateAccountUseCase;
import com.govinda777.execution.business.logic.ProcessAccountProvisioningUseCase;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class ProvisioningSteps {

    @Autowired
    private CreateAccountUseCase createAccountUseCase;

    @Autowired
    private ProcessAccountProvisioningUseCase processAccountProvisioningUseCase;

    @Autowired
    private AccountRepositoryGateway repositoryGateway;

    private CloudAccount inputAccount;
    private CloudAccount createdAccount;
    private CloudAccount seedAccount;

    @Given("a valid request to create a new {string} account named {string} with email {string} and cost center {string}")
    public void setupRequest(String provider, String name, String email, String costCenter) {
        // Garantir que a conta seed exista para o provider
        seedAccount = new CloudAccount();
        seedAccount.setName(provider + "-Seed");
        seedAccount.setProvider(provider);
        seedAccount.setEmail("billing-seed@" + provider.toLowerCase() + ".com");
        seedAccount.setCostCenter("CC-BILLING");
        seedAccount.setState(AccountState.ACTIVE);
        seedAccount = repositoryGateway.save(seedAccount);

        inputAccount = new CloudAccount();
        inputAccount.setName(name);
        inputAccount.setEmail(email);
        inputAccount.setProvider(provider);
        inputAccount.setCostCenter(costCenter);
    }

    @When("the account creation request is submitted")
    public void submitRequest() {
        createdAccount = createAccountUseCase.execute(inputAccount);
        // Simular o trigger assíncrono para fins de teste síncrono no BDD
        processAccountProvisioningUseCase.execute(createdAccount.getId());
    }

    @Then("the account state should be updated to {string}")
    public void checkState(String expectedState) {
        CloudAccount fresh = repositoryGateway.findById(createdAccount.getId()).orElseThrow();
        if (!expectedState.equals(fresh.getState().name())) {
            System.err.println("=== TEST FAILURE DETAILS ===");
            System.err.println("State: " + fresh.getState().name());
            System.err.println("Error Message: " + fresh.getErrorMessage());
            System.err.println("============================");
        }
        assertEquals(expectedState, fresh.getState().name());
    }

    @And("the account should be linked to the {string} seed account")
    public void checkBillingLinked(String provider) {
        CloudAccount fresh = repositoryGateway.findById(createdAccount.getId()).orElseThrow();
        assertNotNull(fresh.getSeedAccountId());
        assertEquals(seedAccount.getId(), fresh.getSeedAccountId());
    }
}
