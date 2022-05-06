subscription_id            = "495d3bb4-96c8-4b04-ac21-78a55b4a55e5"
tenant_id                  = "1cde4cd1-e5ba-4934-9c27-8751ec550d76"

#Enviroment variables
environment                = "test"
location                   = "North Central US"
timeZone                   = "Central Standard Time"

#Application Name
appName                    = "copySSRSNonBillableReports"

#tags
created_by                 = "terraformScript"
environment_type           = "test"
friendly_name              = "Workato Error Logs"

storage_account_file_share_name  = "aba-ssrs-reports"
inbound_ssrs_nb_report           = "Funtion-Inbound-ABA-SSRS-Reports"
archived_ssrs_nb_report          = "Funtion-Archived-ABA-SSRS-Reports"
SFTP_host                        = "sftp.centriahealthcare.com"
SFTP_port                        = 22
SFTP_username                    = "greenhouse-uat"
SFTP_password                    = "1SN6DZ6x"
destiny_folder                   = "//DEV/SSRS_NON_BILLABLE_REPORTS/"
cron_exp                         = "0 */5 7-21 * * *"
