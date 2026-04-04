package org.superwindcloud.webstore.model;

import java.time.Instant;
import org.superwindcloud.webstore.domain.InstalledAppStatus;

public record InstalledAppView(
    String slug,
    String name,
    String category,
    String description,
    String accentColor,
    String icon,
    String logoUrl,
    String appHomeUrl,
    InstalledAppStatus status,
    Instant installedAt) {}
