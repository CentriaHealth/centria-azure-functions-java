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

variable "storage_account_file_share_name"{
  type = string
}

variable "inbound_ssrs_nb_report"{
  type = string
}

variable "archived_ssrs_nb_report"{
  type = string
}

variable "SFTP_host"{
  type = string
}

variable "SFTP_port"{
  type = number
}

variable "SFTP_username"{
  type = string
}

variable "SFTP_password"{
  type = string
  sensitive = true
}

variable "destiny_folder"{
  type = string
}

variable "cron_exp"{
  type = string
}

variable "prefix" {
  type    = string
  default = "centria"
}