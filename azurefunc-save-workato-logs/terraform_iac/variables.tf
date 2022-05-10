variable "subscription_id" {
  type = string
}

variable "tenant_id" {
  type = string
}

variable "environment" {
  type = string
}

variable "location" {
  type = string
}

variable "timeZone" {
  type = string
}

variable "appName" {
  type    = string
}

variable "environment_type" {
  type = string
}

variable "friendly_name" {
  type = string
}

variable "created_by" {
  type = string
}

variable "cosmosDB_DatabaseName" {
  type = string
}

variable "cosmosDB_WorkatoErrorLogs" {
  type = string
}

variable "prefix" {
  type    = string
  default = "centria"
}