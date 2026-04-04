package org.superwindcloud.webstore.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.superwindcloud.webstore.domain.AppDefinition;

public interface AppDefinitionRepository extends JpaRepository<AppDefinition, Long> {

  Optional<AppDefinition> findBySlug(String slug);
}
