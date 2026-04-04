package org.superwindcloud.webstore.service;

import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.superwindcloud.webstore.domain.AppDefinition;
import org.superwindcloud.webstore.repository.AppDefinitionRepository;

@Configuration
public class DataSeeder {

  @Bean
  ApplicationRunner seedCatalog(AppDefinitionRepository appDefinitionRepository) {
    return args -> {
      if (appDefinitionRepository.count() > 0) {
        return;
      }

      appDefinitionRepository.saveAll(
          List.of(
              new AppDefinition(
                  "nextcloud",
                  "Nextcloud",
                  "Productivity",
                  "Private cloud storage, collaboration, and file sharing for teams.",
                  "#4da3ff",
                  "NC",
                  "Unknown"),
              new AppDefinition(
                  "vaultwarden",
                  "Vaultwarden",
                  "Security",
                  "Lightweight password manager compatible with Bitwarden clients.",
                  "#6ee7b7",
                  "VW",
                  "Unknown"),
              new AppDefinition(
                  "jellyfin",
                  "Jellyfin",
                  "Media",
                  "Stream and organize your personal media library from one dashboard.",
                  "#f59e0b",
                  "JF",
                  "Unknown"),
              new AppDefinition(
                  "gitea",
                  "Gitea",
                  "Development",
                  "Self-hosted Git service with repositories, issues, and CI integrations.",
                  "#ef4444",
                  "GT",
                  "Unknown"),
              new AppDefinition(
                  "n8n",
                  "n8n",
                  "Automation",
                  "Workflow automation for internal tools, sync jobs, and app integrations.",
                  "#c084fc",
                  "N8",
                  "Unknown"),
              new AppDefinition(
                  "uptime-kuma",
                  "Uptime Kuma",
                  "Monitoring",
                  "Status monitoring for services, endpoints, and infrastructure health.",
                  "#22c55e",
                  "UK",
                  "Unknown")));
    };
  }

  @Bean
  ApplicationRunner syncRuntipiCatalog(RuntipiAppStoreSyncService runtipiAppStoreSyncService) {
    return args -> runtipiAppStoreSyncService.syncIfStale();
  }
}
