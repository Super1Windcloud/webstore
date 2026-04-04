package org.superwindcloud.webstore.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.superwindcloud.webstore.domain.AppDefinition;
import org.superwindcloud.webstore.domain.InstalledApp;
import org.superwindcloud.webstore.domain.UserAccount;

public interface InstalledAppRepository extends JpaRepository<InstalledApp, Long> {

  List<InstalledApp> findAllByUserOrderByInstalledAtDesc(UserAccount user);

  Optional<InstalledApp> findByUserAndAppDefinition(UserAccount user, AppDefinition appDefinition);
}
