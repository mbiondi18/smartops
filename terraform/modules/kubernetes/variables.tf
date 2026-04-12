variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "zone" {
  description = "GCP zone for the GKE cluster"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "vpc_id" {
  description = "VPC network ID"
  type        = string
}

variable "subnet_name" {
  description = "Subnet name"
  type        = string
}

variable "node_count" {
  description = "Initial number of nodes"
  type        = number
  default     = 2
}

variable "max_node_count" {
  description = "Maximum number of nodes for autoscaling"
  type        = number
  default     = 3
}

variable "machine_type" {
  description = "GCE machine type for nodes"
  type        = string
  default     = "e2-medium"
}
