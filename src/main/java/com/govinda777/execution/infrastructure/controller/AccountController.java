package com.govinda777.execution.infrastructure.controller;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.logic.CreateAccountUseCase;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final AccountRepositoryGateway repositoryGateway;

    public AccountController(CreateAccountUseCase createAccountUseCase, AccountRepositoryGateway repositoryGateway) {
        this.createAccountUseCase = createAccountUseCase;
        this.repositoryGateway = repositoryGateway;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        CloudAccount account = new CloudAccount();
        account.setName(request.getName());
        account.setEmail(request.getEmail());
        account.setProvider(request.getProvider());
        account.setCostCenter(request.getCostCenter());
        account.setSeedAccountId(request.getSeedAccountId());

        CloudAccount created = createAccountUseCase.execute(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> list = repositoryGateway.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable("id") Long id) {
        return repositoryGateway.findById(id)
                .map(acc -> ResponseEntity.ok(toResponse(acc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cost-center/{costCenter}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCostCenter(@PathVariable("costCenter") String costCenter) {
        List<AccountResponse> list = repositoryGateway.findByCostCenter(costCenter)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        List<CloudAccount> all = repositoryGateway.findAll();
        
        long total = all.size();
        long inCreation = all.stream().filter(a -> a.getState() == AccountState.CREATED || a.getState() == AccountState.IN_PROVISIONING).count();
        long active = all.stream().filter(a -> a.getState() == AccountState.ACTIVE).count();
        long failed = all.stream().filter(a -> a.getState() == AccountState.FAILED).count();

        Map<String, Long> byCostCenter = all.stream()
                .collect(Collectors.groupingBy(CloudAccount::getCostCenter, Collectors.counting()));

        Map<String, Long> byProvider = all.stream()
                .collect(Collectors.groupingBy(CloudAccount::getProvider, Collectors.counting()));

        DashboardResponse dashboard = new DashboardResponse(total, inCreation, active, failed, byCostCenter, byProvider);
        return ResponseEntity.ok(dashboard);
    }

    private AccountResponse toResponse(CloudAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getEmail(),
                account.getProvider(),
                account.getState().name(),
                account.getSeedAccountId(),
                account.getCostCenter(),
                account.getErrorMessage(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    // DTOs
    public static class CreateAccountRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        private String email;

        @NotBlank(message = "Provider is required")
        private String provider;

        @NotBlank(message = "Cost Center is required")
        private String costCenter;

        private Long seedAccountId;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getCostCenter() { return costCenter; }
        public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
        public Long getSeedAccountId() { return seedAccountId; }
        public void setSeedAccountId(Long seedAccountId) { this.seedAccountId = seedAccountId; }
    }

    public static class AccountResponse {
        private Long id;
        private String name;
        private String email;
        private String provider;
        private String state;
        private Long seedAccountId;
        private String costCenter;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public AccountResponse(Long id, String name, String email, String provider, String state, 
                               Long seedAccountId, String costCenter, String errorMessage, 
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.provider = provider;
            this.state = state;
            this.seedAccountId = seedAccountId;
            this.costCenter = costCenter;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getProvider() { return provider; }
        public String getState() { return state; }
        public Long getSeedAccountId() { return seedAccountId; }
        public String getCostCenter() { return costCenter; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    public static class DashboardResponse {
        private long totalAccounts;
        private long accountsInCreation;
        private long activeAccounts;
        private long failedAccounts;
        private Map<String, Long> accountsByCostCenter;
        private Map<String, Long> accountsByProvider;

        public DashboardResponse(long totalAccounts, long accountsInCreation, long activeAccounts, long failedAccounts,
                                 Map<String, Long> accountsByCostCenter, Map<String, Long> accountsByProvider) {
            this.totalAccounts = totalAccounts;
            this.accountsInCreation = accountsInCreation;
            this.activeAccounts = activeAccounts;
            this.failedAccounts = failedAccounts;
            this.accountsByCostCenter = accountsByCostCenter;
            this.accountsByProvider = accountsByProvider;
        }

        // Getters
        public long getTotalAccounts() { return totalAccounts; }
        public long getAccountsInCreation() { return accountsInCreation; }
        public long getActiveAccounts() { return activeAccounts; }
        public long getFailedAccounts() { return failedAccounts; }
        public Map<String, Long> getAccountsByCostCenter() { return accountsByCostCenter; }
        public Map<String, Long> getAccountsByProvider() { return accountsByProvider; }
    }
}
