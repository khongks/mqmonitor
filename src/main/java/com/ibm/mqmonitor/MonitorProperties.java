package com.ibm.mqmonitor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {
    
    private Integer period;
    private Float thresholdRed;
    private Float thresholdAmber;
    public Integer getPeriod() {
        return period;
    }
    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Float getThresholdRed() {
        return thresholdRed;
    }
    public void setThresholdRed(Float thresholdRed) {
        this.thresholdRed = thresholdRed;
    }

    public Float getThresholdAmber() {
        return thresholdAmber;
    }
    public void setThresholdAmber(Float thresholdAmber) {
        this.thresholdAmber = thresholdAmber;
    }
    @Override
    public String toString() {
        return "MonitorProperties [period=" + period + ", thresholdRed=" + thresholdRed
                + ", thresholdAmber=" + thresholdAmber + "]";
    }



}
