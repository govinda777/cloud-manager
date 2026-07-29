package com.govinda777.execution.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountRepositoryGateway repositoryGateway;

    @BeforeEach
    void setUp() {
        CloudAccount seedAccount = new CloudAccount();
        seedAccount.setId(100L);
        seedAccount.setName("AWS-Seed");
        seedAccount.setProvider("AWS");
        seedAccount.setState(AccountState.ACTIVE);
        seedAccount.setCostCenter("CC-BILLING");

        when(repositoryGateway.findSeedAccount("AWS")).thenReturn(Optional.of(seedAccount));
    }

    @Test
    void shouldCreateAccountEndpoint() throws Exception {
        AccountController.CreateAccountRequest request = new AccountController.CreateAccountRequest();
        request.setName("Marketing-Prod");
        request.setEmail("marketing@domain.com");
        request.setProvider("AWS");
        request.setCostCenter("CC-MARKETING");

        CloudAccount mockedResult = new CloudAccount();
        mockedResult.setId(1L);
        mockedResult.setName("Marketing-Prod");
        mockedResult.setEmail("marketing@domain.com");
        mockedResult.setProvider("AWS");
        mockedResult.setState(AccountState.CREATED);
        mockedResult.setCostCenter("CC-MARKETING");

        when(repositoryGateway.save(any(CloudAccount.class))).thenReturn(mockedResult);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.state").value("CREATED"))
                .andExpect(jsonPath("$.costCenter").value("CC-MARKETING"));
    }

    @Test
    void shouldGetDashboardEndpoint() throws Exception {
        CloudAccount acc = new CloudAccount();
        acc.setName("Dev-Account");
        acc.setProvider("AWS");
        acc.setState(AccountState.ACTIVE);
        acc.setCostCenter("CC-DEV");

        when(repositoryGateway.findAll()).thenReturn(Collections.singletonList(acc));

        mockMvc.perform(get("/accounts/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(1))
                .andExpect(jsonPath("$.activeAccounts").value(1))
                .andExpect(jsonPath("$.accountsByCostCenter.CC-DEV").value(1));
    }
}
