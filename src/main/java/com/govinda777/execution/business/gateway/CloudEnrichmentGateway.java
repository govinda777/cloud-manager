package com.govinda777.execution.business.gateway;

import com.govinda777.execution.business.model.CloudAccount;
import java.util.Map;

public interface CloudEnrichmentGateway {
    Map<String, Object> getEnrichedDetails(CloudAccount account);
}
