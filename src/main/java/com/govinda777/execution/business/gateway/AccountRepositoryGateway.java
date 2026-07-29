package com.govinda777.execution.business.gateway;

import com.govinda777.execution.business.model.CloudAccount;
import java.util.List;
import java.util.Optional;

public interface AccountRepositoryGateway {
    CloudAccount save(CloudAccount account);
    Optional<CloudAccount> findById(Long id);
    Optional<CloudAccount> findSeedAccount(String provider);
    List<CloudAccount> findAll();
    List<CloudAccount> findByCostCenter(String costCenter);
    List<CloudAccount> findByState(String state);
    Optional<CloudAccount> findAvailablePoolAccount(String provider);
}
