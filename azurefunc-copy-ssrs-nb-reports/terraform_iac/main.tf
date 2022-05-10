terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "2.64.0"
    }
  }
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
  tenant_id       = var.tenant_id
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.prefix}-${var.appName}-${var.environment}-rg"
  location = var.location
  tags = {
    Created_By        = var.created_by
    Created_Date_Time = formatdate("YYYY-MMM-DD hh:mm", timestamp())
    Environment_Type  = var.environment_type
    Friendly_Name     = var.friendly_name
  }
}

resource "azurerm_app_service_plan" "asp" {
  name                = "${var.prefix}-${var.appName}-${var.environment}-asp"
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name
  kind                = "Windows"
  tags = {
    Created_By        = var.created_by
    Created_Date_Time = formatdate("YYYY-MMM-DD hh:mm", timestamp())
    Environment_Type  = var.environment_type
    Friendly_Name     = var.friendly_name
  }
  sku {
    tier     = "Basic"
    size     = "B1"
    capacity = 1
  }
}
