package com.govinda777.execution.infrastructure.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govinda777.execution.business.gateway.CloudEnrichmentGateway;
import com.govinda777.execution.business.model.CloudAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class MockCloudEnrichmentAdapter implements CloudEnrichmentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockCloudEnrichmentAdapter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> getEnrichedDetails(CloudAccount account) {
        if (account == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> details = new HashMap<>();

        try {
            if ("AWS".equalsIgnoreCase(account.getProvider())) {
                // Executa 'aws sts get-caller-identity'
                String output = executeCommand("aws", "sts", "get-caller-identity", "--output", "json");
                if (output != null && !output.isEmpty()) {
                    Map<String, Object> sts = objectMapper.readValue(output, Map.class);
                    details.put("accountId", sts.get("Account"));
                    details.put("iamRole", sts.get("Arn"));
                    details.put("billingLinked", true);
                    details.put("monthToDateCost", "Consultando real via Cost Explorer");
                } else {
                    details.put("accountId", "Erro: Token SSO Expirado ou Credenciais Ausentes");
                    details.put("iamRole", "Rode 'aws sso login' no terminal do host");
                    details.put("billingLinked", false);
                    details.put("monthToDateCost", "-");
                }

                details.put("activeQuota", "Disponível na Cloud");
                details.put("securityAlerts", "Conexão de Teste Real");
                
                // Contar buckets S3 reais
                String bucketsOutput = executeCommand("aws", "s3api", "list-buckets", "--query", "length(Buckets)", "--output", "json");
                int s3Count = 0;
                if (bucketsOutput != null && !bucketsOutput.trim().isEmpty()) {
                    try {
                        s3Count = Integer.parseInt(bucketsOutput.trim());
                    } catch (Exception ignored) {}
                }
                
                Map<String, Object> resources = new HashMap<>();
                resources.put("s3Buckets", s3Count);
                resources.put("ec2Instances", "Conectado via CLI");
                details.put("activeResources", resources);

            } else if ("GCP".equalsIgnoreCase(account.getProvider())) {
                // Obter projeto real do GCP configurado
                String gcpProject = System.getenv("GCP_PROJECT");
                if (gcpProject == null || gcpProject.isEmpty()) {
                    gcpProject = "template-eks-cluster-9035dd86"; // fallback
                }

                // Executa 'gcloud projects describe <project> --format=json'
                String output = executeCommand("gcloud", "projects", "describe", gcpProject, "--format=json");
                if (output != null && !output.isEmpty()) {
                    Map<String, Object> proj = objectMapper.readValue(output, Map.class);
                    details.put("projectId", proj.get("projectId"));
                    details.put("projectNumber", proj.get("projectNumber"));
                    details.put("billingStatus", "Ativo (lifecycleState: " + proj.get("lifecycleState") + ")");
                } else {
                    details.put("projectId", gcpProject);
                    details.put("projectNumber", "gcloud indisponível ou projeto não encontrado");
                    details.put("billingStatus", "Erro de autenticação no host");
                }

                // Listar buckets reais do GCP
                String gsOutput = executeCommand("gcloud", "storage", "buckets", "list", "--format=json");
                int gsCount = 0;
                if (gsOutput != null && !gsOutput.trim().isEmpty()) {
                    try {
                        java.util.List<?> list = objectMapper.readValue(gsOutput, java.util.List.class);
                        gsCount = list.size();
                    } catch (Exception ignored) {}
                }

                details.put("monthToDateCost", "Ativo na Cloud");
                details.put("enabledApis", java.util.List.of("compute.googleapis.com", "storage.googleapis.com", "iam.googleapis.com"));
                
                Map<String, Object> resources = new HashMap<>();
                resources.put("storageBuckets", gsCount);
                resources.put("computeVMs", "Conectado via CLI");
                details.put("activeResources", resources);
            }
        } catch (Exception e) {
            log.error("Erro ao enriquecer dados com CLIs reais", e);
            details.put("error", "Erro de conexão com o provedor: " + e.getMessage());
        }

        return details;
    }

    private String executeCommand(String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().putAll(System.getenv());
            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                log.warn("Comando {} falhou com código de saída {}", command[0], exitCode);
                return null;
            }
        } catch (Exception e) {
            log.warn("Erro ao executar comando de nuvem: {}", command[0], e);
            return null;
        }
    }
}
