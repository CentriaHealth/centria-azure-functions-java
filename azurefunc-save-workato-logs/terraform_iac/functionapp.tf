resource "azurerm_function_app" "function" {
  name                       = "${var.prefix}-${var.appName}-${var.environment}-fn"
  location                   = azurerm_resource_group.rg.location
  resource_group_name        = azurerm_resource_group.rg.name
  app_service_plan_id        = azurerm_app_service_plan.asp.id
  storage_account_name       = azurerm_storage_account.storageacc.name
  storage_account_access_key = azurerm_storage_account.storageacc.primary_access_key
  tags = {
    Created_By        = var.created_by
    Created_Date_Time = formatdate("YYYY-MMM-DD hh:mm", timestamp())
    Environment_Type  = var.environment_type
    Friendly_Name     = var.friendly_name
  }
  site_config {
    always_on = true
	java_version = "1.8"
  }
  app_settings = {
    "FUNCTIONS_WORKER_RUNTIME"                                   = "java"
    "WORKATOJLOGPOST_COSMOS_ERROR_LOG_DB_NAME"                   = azurerm_cosmosdb_account.db.name
    "WORKATOJLOGPOST_COSMOS_ERROR_LOG_COLLECTION_NAME"           = var.cosmosDB_DatabaseName
    "WORKATOJLOGPOST_COSMOS_ERROR_LOG_CONNECTION_STRING_SETTING" = "AccountEndpoint= ${azurerm_cosmosdb_account.db.endpoint}; AccountKey=${azurerm_cosmosdb_account.db.primary_master_key};"
  }
}