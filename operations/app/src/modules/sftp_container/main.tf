terraform {
    required_version = ">= 0.14"
}

resource "azurerm_network_profile" "sftp_network_profile" {
  name = "sftp_network_profile"
  location = var.location
  resource_group_name = var.resource_group

  container_network_interface {
    name = "sftp_container_network_interface"
    ip_configuration {
      name = "sftp_container_ip_configuration"
      subnet_id = var.container_subnet_id
    }
  }
}

resource "azurerm_container_group" "sftp_container" {
  name = var.name
  location = var.location
  resource_group_name = var.resource_group
  ip_address_type = "Private"
  network_profile_id = azurerm_network_profile.sftp_network_profile.id
  os_type = "Linux"
  restart_policy = "Always"
  
  container {
    name = var.name
    image = "atmoz/sftp:alpine"
    cpu = 1.0
    memory = 1.5
    
    ports {
      port = 22
      protocol = "TCP"
    }

    environment_variables = {
      "SFTP_USERS" = "foo:pass:::upload"
    }

    volume {
      name = var.name
      share_name = azurerm_storage_share.sftp_share.name
      mount_path = "/home/foo/upload"
      storage_account_name = var.storage_account_name
      storage_account_key = var.storage_account_key
    }
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_storage_share" "sftp_share" {
  name = var.name
  storage_account_name = var.storage_account_name
}

output "name" {
  value = azurerm_container_group.sftp_container.name
}
