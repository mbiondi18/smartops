resource "google_container_cluster" "primary" {
  name     = "${var.project_id}-${var.environment}-gke"
  location = var.zone

  network    = var.vpc_id
  subnetwork = var.subnet_name

  # Remove the default node pool and create a custom one
  remove_default_node_pool = true
  initial_node_count       = 1
  deletion_protection      = false

  ip_allocation_policy {
    cluster_secondary_range_name  = "pods"
    services_secondary_range_name = "services"
  }
}

resource "google_container_node_pool" "primary_nodes" {
  name       = "${var.project_id}-${var.environment}-node-pool"
  location   = var.zone
  cluster    = google_container_cluster.primary.name
  node_count = var.node_count

  node_config {
    machine_type = var.machine_type

    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]
  }

  autoscaling {
    min_node_count = 1
    max_node_count = var.max_node_count
  }
}
