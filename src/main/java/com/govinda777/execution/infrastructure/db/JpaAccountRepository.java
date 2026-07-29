package com.govinda777.execution.infrastructure.db;

import com.govinda777.execution.business.model.AccountState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaAccountRepository extends JpaRepository<AccountJpaEntity, Long> {
    
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.provider = :provider AND a.seedAccountId IS NULL")
    Optional<AccountJpaEntity> findSeedAccount(@Param("provider") String provider);

    List<AccountJpaEntity> findByCostCenter(String costCenter);

    List<AccountJpaEntity> findByState(AccountState state);
}
