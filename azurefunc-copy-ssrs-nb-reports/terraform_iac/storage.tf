resource "azurerm_storage_account" "storageacc" {
  name                     = format("%s%s%sstorage", var.prefix, var.appName, var.environment)
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  allow_blob_public_access = "true"
  min_tls_version          = "TLS1_2"
  tags = {
    Created_By        = var.created_by
    Created_Date_Time = formatdate("YYYY-MMM-DD hh:mm", timestamp())
    Environment_Type  = var.environment_type
    Friendly_Name     = var.friendly_name
  }
  static_website {}
}

resource "azurerm_storage_share" "fileshare" {
  name                 = var.storage_account_file_share_name
  storage_account_name = azurerm_storage_account.storageacc.name
  quota                = 50
}