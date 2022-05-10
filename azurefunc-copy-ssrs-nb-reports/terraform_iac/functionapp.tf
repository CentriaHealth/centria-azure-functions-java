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
    "FUNCTIONS_WORKER_RUNTIME"                       = "java"
    "SSRSNONBREPORT_INBOUND_FOLDER_SSRS_REPORT"      = var.inbound_ssrs_nb_report
    "SSRSNONBREPORT_ARCHIVED_FOLDER_SSRS_REPORT"     = var.archived_ssrs_nb_report
    "SSRSNONBREPORT_FILE_SHARE_SSRS_REPORTS"         = azurerm_storage_share.fileshare.name
    "SSRSNONBREPORT_ACCOUNT_NAME"                    = azurerm_storage_account.storageacc.name
    "SSRSNONBREPORT_ACCOUNT_KEY"                     = azurerm_storage_account.storageacc.primary_access_key
    "SSRSNONBREPORT_SFTP_HOST"                       = var.SFTP_host
    "SSRSNONBREPORT_SFTP_PORT"                       = var.SFTP_port
    "SSRSNONBREPORT_SFTP_USERNAME"                   = var.SFTP_username
    "SSRSNONBREPORT_SFTP_PASSWORD"                   = var.SFTP_password
    "SSRSNONBREPORT_SFTP_DESTINY_FOLDER"             = var.destiny_folder
    "SSRSNONBREPORT_CRON_EXP"                        = var.cron_exp
  }
}