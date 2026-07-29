package com.govinda777.execution.bdd;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.logic.CreateAccountUseCase;
import com.govinda777.execution.business.logic.ProcessAccountProvisioningUseCase;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import com.govinda777.execution.infrastructure.db.JpaAccountRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DisasterRecoverySteps {

    @Autowired
    private CreateAccountUseCase createAccountUseCase;

    @Autowired
    private ProcessAccountProvisioningUseCase processAccountProvisioningUseCase;

    @Autowired
    private AccountRepositoryGateway repositoryGateway;

    @Autowired
    private JpaAccountRepository jpaAccountRepository;

    private CloudAccount lastCreatedAccount;
    private CloudAccount newSeedAccount;

    @Given("the {string} master seed account is deleted from the platform database")
    public void deleteSeedAccount(String provider) {
        // Clear all entries to cleanly simulate missing seed configuration
        jpaAccountRepository.deleteAll();
        
        // Assert it is indeed deleted
        assertTrue(repositoryGateway.findSeedAccount(provider).isEmpty());
    }

    @When("a request to create a new {string} account named {string} is submitted")
    public void submitAccountRequest(String provider, String name) {
        CloudAccount account = new CloudAccount();
        account.setName(name);
        account.setEmail("dev-owner@corporate.com");
        account.setProvider(provider);
        account.setCostCenter("CC-DISASTER-TEST");

        lastCreatedAccount = createAccountUseCase.execute(account);
        processAccountProvisioningUseCase.execute(lastCreatedAccount.getId());
    }

    @Then("the account state of the new request should be updated to {string}")
    public void checkNewRequestState(String expectedState) {
        CloudAccount fresh = repositoryGateway.findById(lastCreatedAccount.getId()).orElseThrow();
        assertEquals(expectedState, fresh.getState().name());
    }

    @And("the error message of the new request should indicate that the seed account is missing")
    public void checkMissingSeedErrorMessage() {
        CloudAccount fresh = repositoryGateway.findById(lastCreatedAccount.getId()).orElseThrow();
        assertNotNull(fresh.getErrorMessage());
        assertTrue(fresh.getErrorMessage().contains("Seed/Billing account not found"));
    }

    @Given("a new {string} master seed account is registered in the database")
    public void registerNewSeedAccount(String provider) {
        newSeedAccount = new CloudAccount();
        newSeedAccount.setName(provider + "-Recovered-Seed");
        newSeedAccount.setProvider(provider);
        newSeedAccount.setEmail("recovered-billing-seed@" + provider.toLowerCase() + ".com");
        newSeedAccount.setCostCenter("CC-BILLING");
        newSeedAccount.setState(AccountState.ACTIVE);
        
        newSeedAccount = repositoryGateway.save(newSeedAccount);
    }

    @And("the new request should be linked to the new {string} seed account")
    public void checkLinkedToNewSeed(String provider) {
        CloudAccount fresh = repositoryGateway.findById(lastCreatedAccount.getId()).orElseThrow();
        assertNotNull(fresh.getSeedAccountId());
        assertEquals(newSeedAccount.getId(), fresh.getSeedAccountId());
    }
}
