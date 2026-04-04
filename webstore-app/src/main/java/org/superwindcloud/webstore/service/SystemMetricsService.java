package org.superwindcloud.webstore.service;

import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.superwindcloud.webstore.model.SystemOverview;

@Service
public class SystemMetricsService {

  private static final double BYTES_PER_GIB = 1024d * 1024d * 1024d;
  private final OperatingSystemMXBean operatingSystemBean;

  public SystemMetricsService() {
    this.operatingSystemBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
  }

  public SystemOverview getSystemOverview() {
    File root = Path.of(".").toAbsolutePath().getRoot().toFile();

    long totalDisk = Math.max(root.getTotalSpace(), 0L);
    long freeDisk = Math.max(root.getUsableSpace(), 0L);
    long usedDisk = Math.max(totalDisk - freeDisk, 0L);

    long totalMemory = Math.max(operatingSystemBean.getTotalMemorySize(), 0L);
    long freeMemory = Math.max(operatingSystemBean.getFreeMemorySize(), 0L);
    long usedMemory = Math.max(totalMemory - freeMemory, 0L);

    double cpuLoad = Math.max(operatingSystemBean.getCpuLoad(), 0d);

    return new SystemOverview(
        buildDiskMetric(usedDisk, totalDisk, root.getPath()),
        buildCpuMetric(cpuLoad),
        buildMemoryMetric(usedMemory, totalMemory),
        resolveHostName());
  }

  private SystemOverview.Metric buildDiskMetric(long usedDisk, long totalDisk, String rootPath) {
    double usedRatio = totalDisk > 0 ? (double) usedDisk / totalDisk : 0d;
    return new SystemOverview.Metric(
        "磁盘空间",
        formatPercent(usedRatio),
        String.format(
            Locale.ROOT, "%s / %s 已使用 (%s)", formatGiB(usedDisk), formatGiB(totalDisk), rootPath));
  }

  private SystemOverview.Metric buildCpuMetric(double cpuLoad) {
    return new SystemOverview.Metric(
        "CPU负载",
        formatPercent(cpuLoad),
        String.format(Locale.ROOT, "%d 核可用", operatingSystemBean.getAvailableProcessors()));
  }

  private SystemOverview.Metric buildMemoryMetric(long usedMemory, long totalMemory) {
    double usedRatio = totalMemory > 0 ? (double) usedMemory / totalMemory : 0d;
    return new SystemOverview.Metric(
        "内存占用",
        formatPercent(usedRatio),
        String.format(Locale.ROOT, "%s / %s 已使用", formatGiB(usedMemory), formatGiB(totalMemory)));
  }

  private String formatGiB(long bytes) {
    return String.format(Locale.ROOT, "%.1f GB", bytes / BYTES_PER_GIB);
  }

  private String formatPercent(double ratio) {
    return String.format(Locale.ROOT, "%.0f%%", ratio * 100d);
  }

  private String resolveHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException ex) {
      return "localhost";
    }
  }
}
