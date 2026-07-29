package com.govinda777.execution.infrastructure.controller;

import com.govinda777.execution.business.gateway.AccountRepositoryGateway;
import com.govinda777.execution.business.gateway.CloudEnrichmentGateway;
import com.govinda777.execution.business.logic.CreateAccountUseCase;
import com.govinda777.execution.business.model.AccountState;
import com.govinda777.execution.business.model.CloudAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Contas Cloud", description = "Endpoints para gerenciamento, provisionamento e monitoramento de contas multi-cloud (CAPE)")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final AccountRepositoryGateway repositoryGateway;
    private final CloudEnrichmentGateway cloudEnrichmentGateway;

    public AccountController(CreateAccountUseCase createAccountUseCase, 
                             AccountRepositoryGateway repositoryGateway,
                             CloudEnrichmentGateway cloudEnrichmentGateway) {
        this.createAccountUseCase = createAccountUseCase;
        this.repositoryGateway = repositoryGateway;
        this.cloudEnrichmentGateway = cloudEnrichmentGateway;
    }

    @PostMapping
    @Operation(summary = "Criar uma nova conta cloud", description = "Provisiona de forma assíncrona uma nova conta na nuvem configurada (AWS, GCP, Azure) vinculando-a a um centro de custo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Conta criada e enfileirada para provisionamento", 
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (parâmetros obrigatórios ausentes ou inválidos)", content = @Content)
    })
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
    @Operation(summary = "Listar todas as contas", description = "Retorna uma lista com todas as contas cadastradas na base de dados.")
    @ApiResponse(responseCode = "200", description = "Lista de contas retornada com sucesso", 
            content = { @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class))) })
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> list = repositoryGateway.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Obter conta por Nome", description = "Retorna os detalhes de uma conta específica através do seu nome único.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conta encontrada com sucesso", 
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class)) }),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada", content = @Content)
    })
    public ResponseEntity<AccountResponse> getAccountByName(
            @Parameter(description = "Nome da conta a ser buscada", required = true, example = "AWS-Master-Seed")
            @PathVariable("name") String name) {
        return repositoryGateway.findByName(name)
                .map(acc -> {
                    Map<String, Object> details = cloudEnrichmentGateway.getEnrichedDetails(acc);
                    return ResponseEntity.ok(toResponse(acc, details));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cost-center/{costCenter}")
    @Operation(summary = "Listar contas por Centro de Custo", description = "Retorna todas as contas que estão vinculadas ao centro de custo especificado.")
    @ApiResponse(responseCode = "200", description = "Contas filtradas retornadas com sucesso", 
            content = { @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class))) })
    public ResponseEntity<List<AccountResponse>> getAccountsByCostCenter(
            @Parameter(description = "Nome/Código do Centro de Custo", required = true, example = "Engenharia-SP")
            @PathVariable("costCenter") String costCenter) {
        List<AccountResponse> list = repositoryGateway.findByCostCenter(costCenter)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Obter dados do Dashboard de Negócio", description = "Consolida métricas em tempo real sobre o total de contas, contas em processo de criação, ativas, falhas, além da distribuição por centro de custo e provedor.")
    @ApiResponse(responseCode = "200", description = "Métricas do dashboard geradas com sucesso", 
            content = { @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardResponse.class)) })
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
        return toResponse(account, null);
    }

    private AccountResponse toResponse(CloudAccount account, Map<String, Object> cloudDetails) {
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
                account.getUpdatedAt(),
                cloudDetails
        );
    }

    // DTOs
    @Schema(description = "Dados para criação e provisionamento de uma nova conta cloud")
    public static class CreateAccountRequest {
        @NotBlank(message = "Name is required")
        @Schema(description = "Nome do projeto ou conta", example = "projeto-alpha-prod")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        @Schema(description = "E-mail do administrador/dono da conta", example = "admin@empresa.com")
        private String email;

        @NotBlank(message = "Provider is required")
        @Schema(description = "Provedor de Nuvem (AWS, GCP ou AZURE)", example = "AWS")
        private String provider;

        @NotBlank(message = "Cost Center is required")
        @Schema(description = "Centro de custo responsável pela conta", example = "Engenharia-SP")
        private String costCenter;

        @Schema(description = "ID da conta semente vinculada (se aplicável)", example = "10")
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

    @Schema(description = "Retorno com os detalhes da conta cloud")
    public static class AccountResponse {
        @Schema(description = "ID único da conta no banco de dados", example = "1")
        private Long id;
        @Schema(description = "Nome do projeto ou conta", example = "projeto-alpha-prod")
        private String name;
        @Schema(description = "E-mail do administrador/dono da conta", example = "admin@empresa.com")
        private String email;
        @Schema(description = "Provedor de Nuvem", example = "AWS")
        private String provider;
        @Schema(description = "Estado atual do ciclo de vida da conta", example = "ACTIVE")
        private String state;
        @Schema(description = "ID da conta semente vinculada", example = "10")
        private Long seedAccountId;
        @Schema(description = "Centro de custo responsável", example = "Engenharia-SP")
        private String costCenter;
        @Schema(description = "Mensagem explicativa em caso de erro de provisionamento", example = "Limite de cotas excedido")
        private String errorMessage;
        @Schema(description = "Data de registro da solicitação")
        private LocalDateTime createdAt;
        @Schema(description = "Data da última modificação")
        private LocalDateTime updatedAt;
        @Schema(description = "Dados dinâmicos enriquecidos em tempo de execução da cloud")
        private Map<String, Object> cloudDetails;

        public AccountResponse(Long id, String name, String email, String provider, String state, 
                               Long seedAccountId, String costCenter, String errorMessage, 
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
            this(id, name, email, provider, state, seedAccountId, costCenter, errorMessage, createdAt, updatedAt, null);
        }

        public AccountResponse(Long id, String name, String email, String provider, String state, 
                               Long seedAccountId, String costCenter, String errorMessage, 
                               LocalDateTime createdAt, LocalDateTime updatedAt,
                               Map<String, Object> cloudDetails) {
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
            this.cloudDetails = cloudDetails;
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
        public Map<String, Object> getCloudDetails() { return cloudDetails; }
    }

    @Schema(description = "Consolidado de métricas do Dashboard de Negócio")
    public static class DashboardResponse {
        @Schema(description = "Total acumulado de contas", example = "15")
        private long totalAccounts;
        @Schema(description = "Contas em fase de provisionamento", example = "2")
        private long accountsInCreation;
        @Schema(description = "Contas totalmente provisionadas e ativas", example = "12")
        private long activeAccounts;
        @Schema(description = "Contas cujo provisionamento falhou", example = "1")
        private long failedAccounts;
        @Schema(description = "Totalização de contas agrupadas por Centro de Custo", example = "{\"Engenharia-SP\": 10, \"Marketing-US\": 5}")
        private Map<String, Long> accountsByCostCenter;
        @Schema(description = "Totalização de contas agrupadas por Provedor Cloud", example = "{\"AWS\": 8, \"GCP\": 5, \"AZURE\": 2}")
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
