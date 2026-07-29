package com.govinda777.execution.infrastructure.db;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountRepositoryAdapter implements AccountRepositoryGateway {

    private final JpaAccountRepository jpaRepository;

    public AccountRepositoryAdapter(JpaAccountRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CloudAccount save(CloudAccount account) {
        AccountJpaEntity jpaEntity = toJpa(account);
        AccountJpaEntity saved = jpaRepository.save(jpaEntity);
        return toDomain(saved);
    }

    @Override
    public Optional<CloudAccount> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CloudAccount> findSeedAccount(String provider) {
        return jpaRepository.findSeedAccount(provider).map(this::toDomain);
    }

    @Override
    public List<CloudAccount> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CloudAccount> findByCostCenter(String costCenter) {
        return jpaRepository.findByCostCenter(costCenter).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CloudAccount> findByState(String state) {
        AccountState s = AccountState.valueOf(state);
        return jpaRepository.findByState(s).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private CloudAccount toDomain(AccountJpaEntity entity) {
        return new CloudAccount(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getProvider(),
                entity.getState(),
                entity.getSeedAccountId(),
                entity.getCostCenter(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AccountJpaEntity toJpa(CloudAccount domain) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setProvider(domain.getProvider());
        entity.setState(domain.getState());
        entity.setSeedAccountId(domain.getSeedAccountId());
        entity.setCostCenter(domain.getCostCenter());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
