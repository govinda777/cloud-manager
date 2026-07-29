package com.govinda777.execution.infrastructure.db;

import com.govinda777.execution.business.model.AccountState;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String provider;

    @Enumerated(EnumType.STRING)
    private AccountState state;

    private Long seedAccountId;
    private String costCenter;

    @Column(length = 2048)
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AccountJpaEntity() {}

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
