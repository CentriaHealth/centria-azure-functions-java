package com.centria.azure_client_case_workato.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

public class KeyVaultService {

    public String getSecretValue(String keyVaultName){
        String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";

        SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        return secretClient.getSecret("asbskedulopushconnectionstring").getValue();
    }

}
