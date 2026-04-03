package org.superwindcloud.webstore.model;

import org.superwindcloud.webstore.domain.InstalledAppStatus;

public record AppStoreCard(
    String slug,
    String name,
    String category,
    String description,
    String accentColor,
    String icon,
    boolean installed,
    InstalledAppStatus status) {}
