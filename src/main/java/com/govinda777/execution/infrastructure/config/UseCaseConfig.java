package com.govinda777.execution.infrastructure.config;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.gateway.CloudProviderGateway;
import com.govinda777.execution.business.gateway.QueueGateway;
import com.govinda777.execution.business.logic.CreateAccountUseCase;
import com.govinda777.execution.business.logic.ProcessAccountProvisioningUseCase;
import com.govinda777.execution.infrastructure.cloud.MockCloudProviderAdapter;
import com.govinda777.execution.infrastructure.db.AccountRepositoryAdapter;
import com.govinda777.execution.infrastructure.db.JpaAccountRepository;
import com.govinda777.execution.infrastructure.messaging.SqsQueueAdapter;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public AccountRepositoryGateway accountRepositoryGateway(JpaAccountRepository jpaAccountRepository) {
        return new AccountRepositoryAdapter(jpaAccountRepository);
    }

    @Bean
    public QueueGateway queueGateway(SqsTemplate sqsTemplate) {
        return new SqsQueueAdapter(sqsTemplate);
    }

    @Bean
    public CloudProviderGateway cloudProviderGateway() {
        return new MockCloudProviderAdapter();
    }

    @Bean
    public CreateAccountUseCase createAccountUseCase(AccountRepositoryGateway accountRepositoryGateway,
                                                     QueueGateway queueGateway) {
        return new CreateAccountUseCase(accountRepositoryGateway, queueGateway);
    }

    @Bean
    public ProcessAccountProvisioningUseCase processAccountProvisioningUseCase(AccountRepositoryGateway accountRepositoryGateway,
                                                                               CloudProviderGateway cloudProviderGateway) {
        return new ProcessAccountProvisioningUseCase(accountRepositoryGateway, cloudProviderGateway);
    }
}
