package com.govinda777.execution.infrastructure.config;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@Configuration
@Profile("!test")
public class DatabaseSeeder implements CommandLineRunner {

    private final AccountRepositoryGateway repositoryGateway;

    public DatabaseSeeder(AccountRepositoryGateway repositoryGateway) {
        this.repositoryGateway = repositoryGateway;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seeding AWS Master Seed Account
        if (repositoryGateway.findSeedAccount("AWS").isEmpty()) {
            CloudAccount awsSeed = new CloudAccount();
            awsSeed.setName("AWS-Master-Seed");
            awsSeed.setEmail("aws-billing-seed@corporate.com");
            awsSeed.setProvider("AWS");
            awsSeed.setState(AccountState.ACTIVE);
            awsSeed.setCostCenter("CC-CORP-BILLING");
            awsSeed.setCreatedAt(LocalDateTime.now());
            awsSeed.setUpdatedAt(LocalDateTime.now());
            repositoryGateway.save(awsSeed);
        }

        // Seeding GCP Master Seed Account
        if (repositoryGateway.findSeedAccount("GCP").isEmpty()) {
            CloudAccount gcpSeed = new CloudAccount();
            gcpSeed.setName("GCP-Master-Seed");
            gcpSeed.setEmail("gcp-billing-seed@corporate.com");
            gcpSeed.setProvider("GCP");
            gcpSeed.setState(AccountState.ACTIVE);
            gcpSeed.setCostCenter("CC-CORP-BILLING");
            gcpSeed.setCreatedAt(LocalDateTime.now());
            gcpSeed.setUpdatedAt(LocalDateTime.now());
            repositoryGateway.save(gcpSeed);
        }

        // Seed AWS Pool Account (READY_TO_BOOK)
        if (repositoryGateway.findByState("READY_TO_BOOK").stream().filter(a -> a.getProvider().equals("AWS")).count() == 0) {
            CloudAccount awsPool = new CloudAccount();
            awsPool.setName("AWS-Pool-Account-Alpha");
            awsPool.setEmail("pool-aws@corporate.com");
            awsPool.setProvider("AWS");
            awsPool.setState(AccountState.READY_TO_BOOK);
            awsPool.setCostCenter("CC-POOL-PREPROVISION");
            awsPool.setCreatedAt(LocalDateTime.now());
            awsPool.setUpdatedAt(LocalDateTime.now());
            repositoryGateway.findSeedAccount("AWS").ifPresent(seed -> awsPool.setSeedAccountId(seed.getId()));
            repositoryGateway.save(awsPool);
        }

        // Seed GCP Pool Account (READY_TO_BOOK)
        if (repositoryGateway.findByState("READY_TO_BOOK").stream().filter(a -> a.getProvider().equals("GCP")).count() == 0) {
            CloudAccount gcpPool = new CloudAccount();
            gcpPool.setName("GCP-Pool-Account-Beta");
            gcpPool.setEmail("pool-gcp@corporate.com");
            gcpPool.setProvider("GCP");
            gcpPool.setState(AccountState.READY_TO_BOOK);
            gcpPool.setCostCenter("CC-POOL-PREPROVISION");
            gcpPool.setCreatedAt(LocalDateTime.now());
            gcpPool.setUpdatedAt(LocalDateTime.now());
            repositoryGateway.findSeedAccount("GCP").ifPresent(seed -> gcpPool.setSeedAccountId(seed.getId()));
            repositoryGateway.save(gcpPool);
        }
    }
}
