/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.keyvault.samples;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.models.SecretItem;
import com.microsoft.azure.keyvault.requests.SetSecretRequest;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.graphrbac.ActiveDirectoryGroup;
import com.microsoft.azure.management.keyvault.*;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;
import com.microsoft.azure.management.samples.Utils;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyOperation;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.requests.CreateKeyRequest;
import com.microsoft.rest.ServiceFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Azure Key Vault sample for managing key vaults -
 *  - Create a key vault
 *  - Authorize an application
 *  - Update a key vault
 *    - alter configurations
 *    - change permissions
 *  - Add a key to vault
 *  - Add a secret to vault
 *  - Retrieve key from vault
 *  - Retrieve secret from vault
 *  - Retrieve all secrets (up to 10) from vault
 *  - Create another key vault
 *  - List key vaults
 *  - Delete a key vault.
 */
public final class ManageKeyVault {

    /**
     * Main function which runs the actual sample.
     * @param azure instance of the azure client
     * @param clientId client id
     * @return true if sample runs successfully
     */
    public static boolean runSample(Azure azure, ApplicationTokenCredentials credentials) {
        final String vaultName1 = SdkContext.randomResourceName("vault1", 20);
        final String vaultName2 = SdkContext.randomResourceName("vault2", 20);
        final String rgName = SdkContext.randomResourceName("rgKV_", 8);
        final String keyName1 = "key-" +  RandomStringUtils.randomAlphanumeric(8);
        final String keyName2 = "key-" +  RandomStringUtils.randomAlphanumeric(8);
        final String secretName1 = "secret-" + RandomStringUtils.randomAlphanumeric(8);
        final String secretValue1 = RandomStringUtils.randomAlphanumeric(16);
        final String secretName2 = "secret-" + RandomStringUtils.randomAlphanumeric(8);
        final String secretValue2 = RandomStringUtils.randomAlphanumeric(16);
        final String secretName3 = "secret-" + RandomStringUtils.randomAlphanumeric(8);
        final String secretValue3 = RandomStringUtils.randomAlphanumeric(16);


        try {
            //============================================================
            // Create a key vault with empty access policy

            System.out.println("Creating a key vault with no Access Policy...");

            Vault vault1 = azure.vaults().define(vaultName1)
                    .withRegion(Region.US_EAST)
                    .withNewResourceGroup(rgName)
                    .withEmptyAccessPolicy()
                    .create();

            System.out.println("Created key vault");
            Utils.print(vault1);

            //============================================================
            // Authorize an application

            System.out.println("Authorizing the application associated with the current service principal...");

            vault1 = vault1.update()
                    .defineAccessPolicy()
                        .forServicePrincipal(credentials.clientId())
                        .allowKeyAllPermissions()
                        .allowSecretAllPermissions()
                        .attach()
                    .defineAccessPolicy()
                        .forObjectId("cd069970-731a-435d-ac26-8d0f5e6d8862")
                        .allowKeyAllPermissions()
                        .allowSecretAllPermissions()
                        .attach()
                    .apply();

            System.out.println("Updated key vault");
            Utils.print(vault1);

            //===========================================================
            // Create KeyVaultClient for interaction with KeyVault
            // using sp credentials

            KeyVaultClient kvClient = new KeyVaultClient(credentials);

            // Example of how to create a key using KeyClient
            KeyBundle keyRSA = kvClient.createKey(new CreateKeyRequest.Builder(vault1.vaultUri(), keyName1, JsonWebKeyType.RSA).build());
            System.out.println("The key Id is: " + keyRSA.key().kid());


            // Example of creating key by directly interacting with the KeyVault
            Key keyBundle = vault1.keys().define(keyName2)
                    .withKeyTypeToCreate(JsonWebKeyType.RSA)
                    .withKeyOperations(JsonWebKeyOperation.ALL_OPERATIONS)
                    .create();
            System.out.println("The key Id is: " + keyBundle.jsonWebKey().kid());

            //===========================================================
            // Add secret to keyvault
            // Example of how to create a secret using KeyVaultClient
            kvClient.setSecret(new SetSecretRequest.Builder(vault1.vaultUri(), secretName1, secretValue1).build());

            //Example: Async example of creating secret in specified vault, null passed for callback
            ServiceFuture<SecretBundle> secretAsync = kvClient.setSecretAsync(new SetSecretRequest.Builder(vault1.vaultUri(),secretName3,secretValue3).build(),null);
            System.out.println("The secret value is: " + secretAsync.get().value());

            // Add secret directly to KeyVault object without using the KeyVaultClient
            Secret secretBundle = vault1.secrets()
                    .define(secretName2)
                    .withValue(secretValue2)
                    .create();

            // Get individual secret from Vault and print
            SecretBundle secret1 = kvClient.getSecret( vault1.vaultUri(), secretName1 );
            System.out.println("Got Secret: "+ secretName1 + " with value=" + secret1.value());

            // Get up to 10 secrets in vault and loop through them printing their value
            PagedList<SecretItem> secretItems = kvClient.listSecrets(vault1.vaultUri(),10);
            for(SecretItem sf: secretItems){
                System.out.println("secret attributes: " + sf.attributes());
                String secret_url = sf.id();
                System.out.println("secret value: " + secret_url);

                SecretBundle secret = kvClient.getSecret(secret_url);
                String secretValue = secret.value();
                System.out.println("Secret in Key Vault Value: " + secretValue);
            }

            //============================================================
            // Update a key vault
            System.out.println("Update a key vault to enable deployments and add permissions to the application...");

            vault1 = vault1.update()
                    .withDeploymentEnabled()
                    .withTemplateDeploymentEnabled()
                    .updateAccessPolicy(vault1.accessPolicies().get(0).objectId())
                        .allowSecretAllPermissions()
                        .parent()
                    .apply();

            System.out.println("Updated key vault");
            // Print the network security group
            Utils.print(vault1);

            //============================================================
            // Create another key vault
            // Different way to create the vault with detailed access
            // policy configured during create
            Vault vault2 = azure.vaults().define(vaultName2)
                    .withRegion(Region.US_EAST2)
                    .withExistingResourceGroup(rgName)
                    .defineAccessPolicy()
                        .forServicePrincipal(credentials.clientId())
                        .allowKeyPermissions(KeyPermissions.LIST)
                        .allowKeyPermissions(KeyPermissions.GET)
                        .allowKeyPermissions(KeyPermissions.DECRYPT)
                        .allowSecretPermissions(SecretPermissions.GET)
                        .attach()
                    .create();

            System.out.println("Created key vault");
            // Print the network security group
            Utils.print(vault2);

            //============================================================
            // List key vaults

            System.out.println("Listing key vaults...");

            for (Vault vault : azure.vaults().listByResourceGroup(rgName)) {
                Utils.print(vault);
            }

            //============================================================
            // Delete key vaults
            //System.out.println("Deleting the key vaults");
            //azure.vaults().deleteById(vault1.id());
            //azure.vaults().deleteById(vault2.id());
            //System.out.println("Deleted the key vaults");

            return true;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                //System.out.println("Deleting Resource Group: " + rgName);
                //azure.resourceGroups().deleteByName(rgName);
                //System.out.println("Deleted Resource Group: " + rgName);
            } catch (NullPointerException npe) {
                System.out.println("Did not create any resources in Azure. No clean up is necessary");
            } catch (Exception g) {
                g.printStackTrace();
            }
        }

        return false;
    }
    /**
     * Main entry point.
     * @param args the parameters
     */
    public static void main(String[] args) {
        try {

            //=============================================================
            // Authenticate
            /* Comment out Experimental AUTH file method
            final File credFile = new File(System.getenv("AZURE_AUTH_LOCATION"));

            Azure azure = Azure.configure()
                    .withLogLevel(LogLevel.BASIC)
                    .authenticate(credFile)
                    .withDefaultSubscription();
            */

            /*ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
                    "afea05bc-6dd7-4d50-a1bd-6039227a6b05" ,
                    "72f988bf-86f1-41af-91ab-2d7cd011db47",
                    "56c10ef7-2aee-4c98-ad7a-6bf9cecd24d2",
                    AzureEnvironment.AZURE);
            */

            // Read in values from JSON file in resource directory.  Must be in the
            // JSON format.  Formatted correctly based on using the following CLI command
            // az ad sp create-for-rbac --sdk-auth
            File credFile;

            try {
                credFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
            } catch (Exception e) {
                throw new Exception("Credentials file cannot be found or read.  Ensure that the system variable AZURE_AUTH_LOCATION is set");
            }

            JSONParser parser = new JSONParser();
            JSONObject data = (JSONObject) parser.parse(
                    new FileReader(credFile));

            String clientId = (String)data.get("clientId");
            String domain = (String)data.get("tenantId");
            String secret = (String)data.get("clientSecret");
            String subscription = (String)data.get("subscriptionId");

            String azureInfo = String.join(
                System.getProperty("line.separator"),
            "Found the following for Azure authentication",
                "clientId=" + clientId,
                "tenantId=" + domain,
                "subscription=" + subscription
            );

            System.out.println(azureInfo);

            ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
                   clientId, domain, secret, AzureEnvironment.AZURE);

            Azure azure = Azure.authenticate(credentials).withSubscription(subscription);

            // Print selected subscription
            System.out.println("Authorized for selected subscription: " + azure.subscriptionId());

            runSample(azure, credentials);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private ManageKeyVault() {
    }
}
