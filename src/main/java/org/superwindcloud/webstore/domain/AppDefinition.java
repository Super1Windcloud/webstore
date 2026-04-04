package org.superwindcloud.webstore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_definitions")
public class AppDefinition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String slug;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 64)
  private String category;

  @Column(nullable = false, length = 255)
  private String description;

  @Column(nullable = false, length = 32)
  private String accentColor;

  @Column(nullable = false, length = 32)
  private String icon;

  @Column(length = 32)
  private String port;

  protected AppDefinition() {}

  public AppDefinition(
      String slug,
      String name,
      String category,
      String description,
      String accentColor,
      String icon,
      String port) {
    this.slug = slug;
    this.name = name;
    this.category = category;
    this.description = description;
    this.accentColor = accentColor;
    this.icon = icon;
    this.port = port;
  }

  public Long getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public String getAccentColor() {
    return accentColor;
  }

  public String getIcon() {
    return icon;
  }

  public String getPort() {
    return port;
  }

  public void updateCatalogMetadata(
      String name,
      String category,
      String description,
      String accentColor,
      String icon,
      String port) {
    this.name = name;
    this.category = category;
    this.description = description;
    this.accentColor = accentColor;
    this.icon = icon;
    this.port = port;
  }
}
