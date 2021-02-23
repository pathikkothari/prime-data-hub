variable "environment" {
    type = string
    description = "Target Environment"
}

variable "resource_group" {
    type = string
    description = "Resource Group Name"
}

variable "resource_prefix" {
    type = string
    description = "Resource Prefix"
}

variable "postgres_user" {
    type = string
    description = "Database Server Username"
    sensitive = true
}

variable "postgres_password" {
    type = string
    description = "Database Server Password"
    sensitive = true
}

variable "az_phd_user" {
    type = string
    description = "AZ Public Health Department Username"
    sensitive = true
}

variable "az_phd_password" {
    type = string
    description = "AZ Public Health Department Password"
    sensitive = true
}

variable "redox_secret" {
    type = string
    description = "Redox Secret"
    sensitive = true
}

variable "okta_client_id" {
    type = string
    description = "Okta Client ID"
    sensitive = true
}

variable "https_cert_name" {
    type = string
    description = "The HTTPS cert to associate with the front door. Omitting will not associate a domain to the front door."
}

variable "okta_redirect_url" {
    type = string
    description = "Okta Redirect URL"
}