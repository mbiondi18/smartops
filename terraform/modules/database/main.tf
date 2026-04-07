resource "google_sql_database_instance" "postgres" {
  name             = "${var.project_id}-${var.environment}-db"
  database_version = "POSTGRES_15"
  region           = var.region

  settings {
    tier              = var.db_tier
    availability_type = var.environment == "prod" ? "REGIONAL" : "ZONAL"

    backup_configuration {
      enabled = var.environment == "prod" ? true : false
    }

    ip_configuration {
      ipv4_enabled = true
    }
  }

  deletion_protection = var.environment == "prod" ? true : false
}

resource "google_sql_database" "smartops" {
  name     = "smartops"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "smartops" {
  name     = "smartops"
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
}
