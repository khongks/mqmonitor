package com.ibm.mqmonitor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;

@Component
public class MQQueueMonitor {

    static final Logger log = LoggerFactory.getLogger(MQQueueMonitor.class);

    private TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
            }
        }
    };
    private SSLContext sslContext;

    private MQQueueManager queueManager;
    private MQProperties mqProperties;
    private SplunkProperties splunkProperties;
    private MonitorProperties monitorProperties;
    private PCFMessageAgent agent = null;

    private String splunkAuthorization;
    private String splunkEndpoint;

    public MQQueueMonitor(MQProperties mqProperties, SplunkProperties splunkProperties, MonitorProperties monitorProperties) {
        this.mqProperties = mqProperties;
        this.splunkProperties = splunkProperties;
        this.monitorProperties = monitorProperties;

        // need to strip off trailing newline, when reading from secrets
        splunkAuthorization = splunkProperties.getToken();
        splunkAuthorization = "Splunk " + splunkAuthorization.replaceAll("\r", "").replaceAll("\n", "");
        splunkEndpoint = splunkProperties.getEndpoint();
        splunkEndpoint = splunkEndpoint.replaceAll("\r", "").replaceAll("\n", "");

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {
		log.info("connect: " + mqProperties.toString());
        log.info("connect: " + splunkProperties.toString());
        log.info("connect: " + monitorProperties.toString());
        try {
            Hashtable<String,Object> mqht = new Hashtable<String, Object>();
            mqht.put(CMQC.CHANNEL_PROPERTY, mqProperties.getChannelName().toString());
            mqht.put(CMQC.HOST_NAME_PROPERTY, mqProperties.getHostname());
            mqht.put(CMQC.PORT_PROPERTY, mqProperties.getPort().intValue());
            mqht.put(CMQC.USER_ID_PROPERTY, mqProperties.getUser());
            mqht.put(CMQC.PASSWORD_PROPERTY, mqProperties.getPassword());
            queueManager = new MQQueueManager(mqProperties.getQueueManagerName()+"", mqht);
            //queueManager = new MQQueueManager("QMLAB1", mqht);
            log.info("MQQueueManager created: " + queueManager.getName());

            agent = new PCFMessageAgent(queueManager);
            log.info("PCFMessageAgent created");
            reconnect = false;
        } 
        catch (MQException mqe) {
            mqe.printStackTrace();
            log.error("MQException: CC=" + mqe.completionCode + " : RC=" + mqe.reasonCode + " Msg=" + mqe.getLocalizedMessage());
        } 
        catch (MQDataException mqde) {
            log.error("MQDataException: CC=" + mqde.completionCode + " : RC=" + mqde.reasonCode);
        }
        // finally {
        //     try {
        //         if (this.agent != null) {
        //             this.agent.disconnect();
        //             log.info("disconnected from agent");
        //         }
        //     } catch (MQDataException mqde) {
        //         log.error("MQDataException: CC=" + mqde.completionCode + " : RC=" + mqde.reasonCode + " Msg=" + mqde.getLocalizedMessage());
        //     }
        //     try {
        //         if(this.queueManager != null) {
        //             this.queueManager.disconnect();
        //             log.error("disconnected from "+ mqProperties.getQueueManagerName());
        //         }
        //     } 
        //     catch (MQException mqe) {
        //         log.error("MQException: CC=" + mqe.completionCode + " : RC=" + mqe.reasonCode + " Msg=" + mqe.getLocalizedMessage());
        //     }
        // }
    }

    private boolean reconnect = false;

    public void doPCF() {
        if(reconnect) {
            connect();
        }
        try {
            // https://www.ibm.com/docs/en/ibm-mq/9.3?topic=formats-mqcmd-inquire-q-inquire-queue
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
            // Add generic queue name. E.g. *TEST
            request.addParameter(CMQC.MQCA_Q_NAME, mqProperties.getGenericQueueName());
            // Add parameter to request only local queues
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
            // Add parameter to request only queue name, current depth and max depth
            request.addParameter(CMQCFC.MQIACF_Q_ATTRS, new int [] {
                CMQC.MQCA_Q_NAME,
                CMQC.MQIA_CURRENT_Q_DEPTH,
                CMQC.MQIA_MAX_Q_DEPTH
            });
            // Add filter to only return responses with a queue depth greater than 0 (zero) i.e. non-zero queue depth
            request.addFilterParameter(CMQC.MQIA_CURRENT_Q_DEPTH, CMQCFC.MQCFOP_GREATER, 0);
            PCFMessage[] responses = this.agent.send(request);
         
            for (int i = 0; i < responses.length; i++) {
                if ( ((responses[i]).getCompCode() == CMQC.MQCC_OK) &&
                     ((responses[i]).getParameterValue(CMQC.MQCA_Q_NAME) != null) ) {
                    String name = responses[i].getStringParameterValue(CMQC.MQCA_Q_NAME);
                    if (name != null) {
                        name = name.trim();
                    }
                    int curDepth = responses[i].getIntParameterValue(CMQC.MQIA_CURRENT_Q_DEPTH);
                    int maxDepth = responses[i].getIntParameterValue(CMQC.MQIA_MAX_Q_DEPTH);
                    log.info("Name="+name + " : curDepth=" + curDepth + ", maxDepth=" + maxDepth + ", thresholdRed: " + monitorProperties.getThresholdRed().floatValue() + ", thresholdAmber: " + (float)monitorProperties.getThresholdAmber().floatValue());
                    if (curDepth == maxDepth) {
                        String message = "Error: Name="+name + " : current depth equals max depth ["+maxDepth+"]";
                        log.error(message);
                        sendToSplunk(message);
                    } else if (curDepth >= (maxDepth * monitorProperties.getThresholdRed().floatValue())) {
                        String message = "Warning: Name="+name + " : current depth ["+curDepth+"] is above " + monitorProperties.getThresholdRed().floatValue()*100+ "% of max depth ["+maxDepth+"]";
                        log.warn(message);
                        sendToSplunk(message);
                    } else if (curDepth >= (maxDepth * monitorProperties.getThresholdAmber().floatValue())) {
                        String message = "Info: Name="+name + " : current depth ["+curDepth+"] is above " + monitorProperties.getThresholdAmber().floatValue()*100 + "% of max depth ["+maxDepth+"]";
                        log.info(message);
                        sendToSplunk(message);
                    }
                }
            }

        } catch(IOException ioe) {
            log.error("IOException:" +ioe.getLocalizedMessage());
            reconnect = true;
        } catch (PCFException pcfe) {
            log.error("PCFException: CC=" + pcfe.completionCode + " : RC=" + pcfe.reasonCode);
            reconnect = true;
        } catch (MQDataException mqde) {
            log.error("MQDataException: CC=" + mqde.completionCode + " : RC=" + mqde.reasonCode);
            reconnect = true;
        }
    }

    // private void sendToSplunk(String message) {
    //     try {
    //         SslContext sslContext = SslContextBuilder
    //             .forClient()
    //             .trustManager(InsecureTrustManagerFactory.INSTANCE)
    //             .build();
    //         //HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

    //         WebClient webClient = WebClient.builder()
    //         .baseUrl(splunkProperties.getEndpoint())
    //         .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
    //         .defaultHeader(HttpHeaders.AUTHORIZATION, "Splunk " + splunkProperties.getToken())
    //         .build();
            
    //     } catch (SSLException e) {
    //         log.error("SSLException: " + e.getLocalizedMessage());
    //     }
    // }

    private void sendToSplunk(String message) {
        try {
            final Properties props = System.getProperties(); 
            props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

            String jsonBody = "{\"event\": \"" + message + "\"}";
            log.info(jsonBody);
            HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(splunkEndpoint))
                        .headers("Authorization", splunkAuthorization)
                        .headers("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
            HttpClient client = HttpClient.newBuilder()
                .sslContext(this.sslContext)
                .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode == 200 || statusCode == 201) {
                log.info("Send to Splunk successful");
            } else {
                log.error("Send to Splunk error: " + statusCode);
            }

        } catch (URISyntaxException urie) {
            log.error("URISyntaxException: Error: "  + urie.getLocalizedMessage());
            urie.printStackTrace();
        } catch (IOException ioe) {
            log.error("IOException: Error: "  + ioe.getLocalizedMessage());
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            log.error("InterruptedException: Error: " + ie.getLocalizedMessage());
            ie.printStackTrace();
        }
    }

    public MQProperties getMqProperties() {
        return mqProperties;
    }
    public void setMqProperties(MQProperties mqProperties) {
        this.mqProperties = mqProperties;
    }
}
