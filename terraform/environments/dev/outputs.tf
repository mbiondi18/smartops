output "registry_url" {
  description = "Artifact Registry URL"
  value       = module.registry.repository_url
}

output "cluster_name" {
  description = "GKE cluster name"
  value       = module.kubernetes.cluster_name
}

output "db_connection_name" {
  description = "Cloud SQL connection name"
  value       = module.database.connection_name
}
