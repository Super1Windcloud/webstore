package org.superwindcloud.webstore.model;

public record DashboardSummary(
    long totalApps, long installedApps, long runningApps, long stoppedApps) {}
