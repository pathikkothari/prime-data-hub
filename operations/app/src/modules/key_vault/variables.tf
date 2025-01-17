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

variable "location" {
  type = string
  description = "Network Location"
}

variable "endpoint_subnet_id" {
  type = string
  description = "Private Endpoint Subnet ID"
}