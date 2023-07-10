package com.ibm.mqmonitor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {
    
    private Integer period;
    private Integer monitorThresholdRed;
    private Integer monitorThresholdOrange;
    public Integer getPeriod() {
        return period;
    }
    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Integer getMonitorThresholdRed() {
        return monitorThresholdRed;
    }
    public void setMonitorThresholdRed(Integer monitorThresholdRed) {
        this.monitorThresholdRed = monitorThresholdRed;
    }

    public Integer getMonitorThresholdOrange() {
        return monitorThresholdOrange;
    }
    public void setMonitorThresholdOrange(Integer monitorThresholdOrange) {
        this.monitorThresholdOrange = monitorThresholdOrange;
    }
    @Override
    public String toString() {
        return "MonitorProperties [period=" + period + ", monitorThresholdRed=" + monitorThresholdRed
                + ", monitorThresholdOrange=" + monitorThresholdOrange + "]";
    }



}
