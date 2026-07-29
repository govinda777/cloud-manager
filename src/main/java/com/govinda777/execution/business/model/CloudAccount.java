package com.govinda777.execution.business.model;

import java.time.LocalDateTime;

public class CloudAccount {
    private Long id;
    private String name;
    private String email;
    private String provider; // "AWS" or "GCP"
    private AccountState state;
    private Long seedAccountId;
    private String costCenter;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CloudAccount() {}

    public CloudAccount(Long id, String name, String email, String provider, AccountState state, 
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

    // Business behaviors
    public boolean isSeedAccount() {
        return seedAccountId == null;
    }

    public void startProvisioning() {
        if (this.state != AccountState.CREATED && this.state != AccountState.FAILED) {
            throw new IllegalStateException("Cannot start provisioning from state: " + this.state);
        }
        this.state = AccountState.IN_PROVISIONING;
        this.updatedAt = LocalDateTime.now();
    }

    public void linkBilling(Long seedAccountId) {
        if (this.state != AccountState.IN_PROVISIONING) {
            throw new IllegalStateException("Cannot link billing from state: " + this.state);
        }
        this.seedAccountId = seedAccountId;
        this.state = AccountState.BILLING_LINKED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markReadyToBook() {
        if (this.state != AccountState.BILLING_LINKED && this.state != AccountState.IN_PROVISIONING) {
            throw new IllegalStateException("Cannot transition to READY_TO_BOOK from state: " + this.state);
        }
        this.state = AccountState.READY_TO_BOOK;
        this.updatedAt = LocalDateTime.now();
    }

    public void book(String name, String email, String costCenter) {
        if (this.state != AccountState.READY_TO_BOOK) {
            throw new IllegalStateException("Cannot book account in state: " + this.state);
        }
        this.name = name;
        this.email = email;
        this.costCenter = costCenter;
        this.state = AccountState.BOOKED;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        if (this.state != AccountState.BILLING_LINKED && this.state != AccountState.IN_PROVISIONING && this.state != AccountState.BOOKED) {
            throw new IllegalStateException("Cannot activate account from state: " + this.state);
        }
        this.state = AccountState.ACTIVE;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.state = AccountState.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public AccountState getState() { return state; }
    public void setState(AccountState state) { this.state = state; }

    public Long getSeedAccountId() { return seedAccountId; }
    public void setSeedAccountId(Long seedAccountId) { this.seedAccountId = seedAccountId; }

    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
