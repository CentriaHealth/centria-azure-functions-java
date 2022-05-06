resource "azurerm_cosmosdb_account" "db" {
  name                              = "${var.prefix}-${var.appName}-${var.environment}-db"
  location                          = var.location
  resource_group_name               = azurerm_resource_group.rg.name
  offer_type                        = "Standard"
  kind                              = "GlobalDocumentDB"
  is_virtual_network_filter_enabled = true
  ip_range_filter                   = "0.0.0.0"
  enable_automatic_failover         = false
  tags = {
    Created_By        = var.created_by
    Created_Date_Time = formatdate("YYYY-MMM-DD hh:mm", timestamp())
    Environment_Type  = var.environment_type
    Friendly_Name     = var.friendly_name
  }

  consistency_policy {
    consistency_level       = "Session"
    max_interval_in_seconds = 5
    max_staleness_prefix    = 100
  }

  geo_location {
    location          = var.location
    failover_priority = 0
  }

  backup {
    type                = "Periodic"
    interval_in_minutes = 240
    retention_in_hours  = 8
  }
}

data "azurerm_cosmosdb_account" "db" {
  name                = azurerm_cosmosdb_account.db.name
  resource_group_name = azurerm_resource_group.rg.name
}

resource "azurerm_cosmosdb_sql_database" "database" {
  name                = var.cosmosDB_DatabaseName
  resource_group_name = azurerm_cosmosdb_account.db.resource_group_name
  account_name        = azurerm_cosmosdb_account.db.name
  throughput          = 400
}

resource "azurerm_cosmosdb_sql_container" "logs" {
  name                  = var.cosmosDB_WorkatoErrorLogs
  resource_group_name   = azurerm_cosmosdb_account.db.resource_group_name
  account_name          = azurerm_cosmosdb_account.db.name
  database_name         = azurerm_cosmosdb_sql_database.database.name
  partition_key_path    = "/id"
  partition_key_version = 1
  throughput            = 400

  indexing_policy {
    indexing_mode = "Consistent"

    included_path {
      path = "/*"
    }

    excluded_path {
      path = "/\"_etag\"/?"
    }
  }

  conflict_resolution_policy {
    mode                     = "LastWriterWins"
    conflict_resolution_path = "/_ts"
  }
}