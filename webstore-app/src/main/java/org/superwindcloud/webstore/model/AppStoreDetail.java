package org.superwindcloud.webstore.model;

import java.util.List;
import org.superwindcloud.webstore.domain.InstalledAppStatus;

public record AppStoreDetail(
    String slug,
    String name,
    String category,
    String description,
    String longDescription,
    String metadataDescription,
    String metadataDescriptionHtml,
    String accentColor,
    String icon,
    String logoUrl,
    String appHomeUrl,
    boolean installed,
    InstalledAppStatus status,
    String version,
    String author,
    String sourceUrl,
    String port,
    String tipiVersion,
    List<String> architectures) {}
