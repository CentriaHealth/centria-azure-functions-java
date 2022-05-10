resource "azurerm_application_insights" "appinsights" {
  name                = "${var.prefix}-${var.appName}-${var.environment}-ai"
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "java"
  tags = {
    Created_By        = var.created_by
    Created_Date_Time = formatdate("YYYY-MMM-DD hh:mm", timestamp())
    Environment_Type  = var.environment_type
    Friendly_Name     = var.friendly_name
  }
}