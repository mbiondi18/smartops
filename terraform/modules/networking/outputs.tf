output "vpc_id" {
  description = "The VPC network ID"
  value       = google_compute_network.vpc.id
}

output "subnet_id" {
  description = "The subnet ID"
  value       = google_compute_subnetwork.subnet.id
}

output "subnet_name" {
  description = "The subnet name"
  value       = google_compute_subnetwork.subnet.name
}
