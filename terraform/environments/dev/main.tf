terraform {
  required_version = ">= 1.7.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }

  backend "gcs" {
    bucket = "smartops-hub-project-tfstate"
    prefix = "dev"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

module "networking" {
  source      = "../../modules/networking"
  project_id  = var.project_id
  region      = var.region
  environment = var.environment
}

module "registry" {
  source      = "../../modules/registry"
  project_id  = var.project_id
  region      = var.region
  environment = var.environment
}

module "database" {
  source      = "../../modules/database"
  project_id  = var.project_id
  region      = var.region
  environment = var.environment
  db_password = var.db_password
}

module "kubernetes" {
  source      = "../../modules/kubernetes"
  project_id  = var.project_id
  zone        = var.zone
  environment = var.environment
  vpc_id      = module.networking.vpc_id
  subnet_name = module.networking.subnet_name
}
