---
services: Keyvault
platforms: java
author: jianghaolu, brentmcconnell
---

## Getting Started with Keyvault - Manage Key Vault - in Java ##


  Azure Key Vault sample for managing key vaults -
   - Create a resource group
   - Create a key vault
   - Authorize an application
   - Add keys to key vault
   - Read keys from key vault
   - Add secrets to key vault
   - Read secrets from key vault
   - Update a key vault
     - alter configurations
     - change permissions
   - Create another key vault
   - List key vaults
   - Delete a key vault
   - Delete a resource group
 

## Running this Sample ##

To run this sample:

There are 2 branches in this repo:
1.  master = Azure Commerical
2.  gov = Azure Government

Checkout the appropriate branch before completing the rest of these instructions.

Ensure you have an active Azure subscription and that the `az` CLI has been installed
and authenticated.

Set the environment variable `AZURE_AUTH_LOCATION` with the full path for an auth file.
To create an auth file use the Azure CLI and run the following command:

    `az ad sp create-for-rbac --sdk-auth > my.azureauth`

Once the file has been created.  Create an environment variable called AZURE_AUTH_LOCATION
that points to the full pathname to the file.

    git clone https://github.com/Azure-Samples/key-vault-java-manage-key-vaults.git

    cd key-vault-java-manage-key-vaults

    mvn clean compile exec:java

By default this program will run all of the above samples and then delete the keyvault and resource group.  If you would
like to keep the keyvaults and resource group you can run the following:

    mvn clean compile exec:java -Dexec.args="--keep"

This will perform the same samples as above but leave the keyvault and resource group intact.  If you would like to
browse the keyvaults via the portal you should give your account the correct Access Policies to view the contents of the
keyvaults. From the CLI you can see the secrets in the vault using the following:

    az keyvault list -> To identify the keyvaults created
    az keyvault key list --vault-name XXXXXX -o table -> where -n is the keyvault name
    az keyvault secret list --vault-name XXXXXXX --query '[].{secretID:id}' -o table -> where --vault-name is the keyvault name

## More information ##

[http://azure.com/java](http://azure.com/java)

If you don't have a Microsoft Azure subscription you can get a FREE trial account [here](http://go.microsoft.com/fwlink/?LinkId=330212)

---

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.