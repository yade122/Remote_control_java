package shared;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SystemInfo implements Serializable {
    private String hostname;
    private String os;
    private int cpuCores;
    private double cpuUsage;
    private long totalMemory;
    private long usedMemory;
    private LocalDateTime timestamp;
    private String status;

    public SystemInfo(String hostname, String os, int cpuCores, double cpuUsage, 
                     long totalMemory, long usedMemory, String status) {
        this.hostname = hostname;
        this.os = os;
        this.cpuCores = cpuCores;
        this.cpuUsage = cpuUsage;
        this.totalMemory = totalMemory;
        this.usedMemory = usedMemory;
        this.timestamp = LocalDateTime.now();
        this.status = status;
    }

    // Getters and setters
    public String getHostname() { return hostname; }
    public String getOs() { return os; }
    public int getCpuCores() { return cpuCores; }
    public double getCpuUsage() { return cpuUsage; }
    public long getTotalMemory() { return totalMemory; }
    public long getUsedMemory() { return usedMemory; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    
    public double getMemoryUsagePercentage() {
        return (totalMemory > 0) ? (usedMemory * 100.0 / totalMemory) : 0;
    }
}