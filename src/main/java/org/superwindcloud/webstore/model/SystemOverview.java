package org.superwindcloud.webstore.model;

public record SystemOverview(
    Metric diskSpace, Metric cpuLoad, Metric memoryUsage, String hostName) {

  public record Metric(String title, String primaryValue, String secondaryValue) {}
}
