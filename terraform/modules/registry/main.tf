resource "google_artifact_registry_repository" "repo" {
  location      = var.region
  repository_id = "${var.project_id}-${var.environment}"
  format        = "DOCKER"
  description   = "SmartOps Hub Docker repository"
}
