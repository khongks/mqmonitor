package com.ibm.mqmonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MQMonitorApplication implements CommandLineRunner {

    static final Logger log = LoggerFactory.getLogger(MQMonitorApplication.class);

	@Autowired
	private MQQueueMonitor monitor;

    @Autowired
    private MQProperties mqProperties;

    @Autowired
    private SplunkProperties splunkProperties;

	@Autowired
	private MonitorProperties monitorProperties;

	public static void main(String[] args) {
		SpringApplication.run(MQMonitorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.info(mqProperties.toString());
		log.info(splunkProperties.toString());
		log.info(monitorProperties.toString());
		// MQQueueMonitor monitor = new MQQueueMonitor();
		monitor.connect();
		for(;;) {
			log.info("Start monitoring");
			monitor.doPCF();
			try { Thread.sleep(monitorProperties.getPeriod().intValue()*1000); } catch(Exception e) {}
		}
	}

}
