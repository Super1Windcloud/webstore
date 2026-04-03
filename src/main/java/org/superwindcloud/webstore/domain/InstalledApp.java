package org.superwindcloud.webstore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "installed_apps",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "app_definition_id"}))
public class InstalledApp {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "app_definition_id", nullable = false)
  private AppDefinition appDefinition;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private InstalledAppStatus status;

  @Column(nullable = false)
  private Instant installedAt;

  protected InstalledApp() {}

  public InstalledApp(UserAccount user, AppDefinition appDefinition, InstalledAppStatus status) {
    this.user = user;
    this.appDefinition = appDefinition;
    this.status = status;
  }

  @PrePersist
  void prePersist() {
    if (installedAt == null) {
      installedAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public UserAccount getUser() {
    return user;
  }

  public AppDefinition getAppDefinition() {
    return appDefinition;
  }

  public InstalledAppStatus getStatus() {
    return status;
  }

  public void setStatus(InstalledAppStatus status) {
    this.status = status;
  }

  public Instant getInstalledAt() {
    return installedAt;
  }
}
