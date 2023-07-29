package com.ibm.mqmonitor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ibm.mq.MQEnvironment;
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
                log.info("TrustManager.getAcceptedIssuers");
                return null;
            }
            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
                log.info("TrustManager.checkClientTrusted");
            }
            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
                log.info("TrustManager.checkServerTrusted");
            }
        }
    };

    // private KeyManager[] keys = new KeyManager[] {
    //     new X509KeyManager() {
            
    //     };
    // };

    private SSLContext sslContext;
    private SSLContext mqSslContext;
    private MQQueueManager queueManager;
    private MQProperties mqProperties;
    private SplunkProperties splunkProperties;
    private MonitorProperties monitorProperties;
    private TLSProperties tlsProperties;
    private PCFMessageAgent agent;
    private String splunkAuthorization;
    private String splunkEndpoint;


    public MQQueueMonitor(MQProperties mqProperties, SplunkProperties splunkProperties, MonitorProperties monitorProperties, TLSProperties tlsProperties) {
        this.mqProperties = mqProperties;
        this.splunkProperties = splunkProperties;
        this.monitorProperties = monitorProperties;
        this.tlsProperties = tlsProperties;

		log.info("connect: " + mqProperties.toString());
		log.info("connect: " + tlsProperties.toString());
        log.info("connect: " + splunkProperties.toString());
        log.info("connect: " + monitorProperties.toString());

        // need to strip off trailing newline, when reading from secrets
        splunkAuthorization = this.splunkProperties.getToken();
        splunkAuthorization = "Splunk " + this.splunkAuthorization.replaceAll("\r", "").replaceAll("\n", "");
        splunkEndpoint = this.splunkProperties.getEndpoint();
        splunkEndpoint = splunkEndpoint.replaceAll("\r", "").replaceAll("\n", "");

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            switch(tlsProperties.getMode()) {
                case "tls": {
                    if (tlsProperties.getDebug()) {
                        System.setProperty("javax.net.debug", "ssl:handshake");
                    }
                    // System.setProperty("javax.net.ssl.trustStore", tlsProperties.getTruststoreProperties().getFilename());
                    // System.setProperty("javax.net.ssl.trustStorePassword", tlsProperties.getTruststoreProperties().getPassword());
                    // System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
                    System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");
                    TrustManagerFactory tmf = initMQTrustStore();
                    mqSslContext = SSLContext.getInstance("TLS");
                    mqSslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
                    MQEnvironment.sslSocketFactory = mqSslContext.getSocketFactory();
                    MQEnvironment.sslCipherSuite = tlsProperties.getCipherSuite();
                }
                case "mtls": {
                    if (tlsProperties.getDebug()) {
                        System.setProperty("javax.net.debug", "ssl:handshake");
                    }
                    // System.setProperty("javax.net.ssl.keyStore", tlsProperties.getKeystoreProperties().getFilename());
                    // System.setProperty("javax.net.ssl.keyStorePassword", tlsProperties.getKeystoreProperties().getPassword());
                    // System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
                    // System.setProperty("javax.net.ssl.trustStore", tlsProperties.getTruststoreProperties().getFilename());
                    // System.setProperty("javax.net.ssl.trustStorePassword", tlsProperties.getTruststoreProperties().getPassword());
                    // System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
                    System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");
                    KeyManagerFactory kmf = initMQKeyStore();
                    TrustManagerFactory tmf = initMQTrustStore();
                    mqSslContext = SSLContext.getInstance("TLS");
                    mqSslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
                    MQEnvironment.sslSocketFactory = mqSslContext.getSocketFactory();
                    MQEnvironment.sslCipherSuite = tlsProperties.getCipherSuite();
                }
                case "none":
                default:
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public KeyManagerFactory initMQKeyStore() throws NoSuchAlgorithmException, CertificateException, IOException, FileNotFoundException, KeyStoreException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(tlsProperties.getKeystoreProperties().getType());
        keyStore.load(new FileInputStream(tlsProperties.getKeystoreProperties().getFilename()), tlsProperties.getKeystoreProperties().getPassword().toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, tlsProperties.getKeystoreProperties().getPassword().toCharArray());
        return kmf;
    }

    public TrustManagerFactory initMQTrustStore() throws NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, KeyStoreException {
        KeyStore trustStore = KeyStore.getInstance(tlsProperties.getTruststoreProperties().getType());
        trustStore.load(new FileInputStream(tlsProperties.getTruststoreProperties().getFilename()), tlsProperties.getTruststoreProperties().getPassword().toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf;
    }

    public void connect() {
        try {
            MQEnvironment.hostname = mqProperties.getHostname();
            MQEnvironment.port = mqProperties.getPort().intValue();
            MQEnvironment.channel = mqProperties.getChannelName();
            MQEnvironment.userID = mqProperties.getUser();
            MQEnvironment.password = mqProperties.getPassword();
            queueManager = new MQQueueManager(mqProperties.getQueueManagerName());
            log.info("MQQueueManager created: " + queueManager.getName());

            agent = new PCFMessageAgent(queueManager);
            log.info("Connected to queue manager");
            reconnect = false;
        } 
        catch (MQException | MQDataException mqe) {
            log.error("MQException: " + mqe.getCause() + " Msg=" + mqe.getLocalizedMessage());
            reconnect = true;
        } 
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
            if (agent != null) {
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
            } else {
                log.error("Agent is not created yet. Need to reconnect.");
                reconnect = true;
            }

        } catch(IOException ioe) {
            log.error("IOException:" +ioe.getLocalizedMessage());
            ioe.printStackTrace();
            reconnect = true;
        } catch (PCFException pcfe) {
            log.error("PCFException: CC=" + pcfe.completionCode + " : RC=" + pcfe.reasonCode);
            pcfe.printStackTrace();
            reconnect = true;
        } catch (MQDataException mqde) {
            log.error("MQDataException: CC=" + mqde.completionCode + " : RC=" + mqde.reasonCode);
            mqde.printStackTrace();
            reconnect = true;
        }
    }

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
    public TLSProperties getTlsProperties() {
        return tlsProperties;
    }
    public void setTlsProperties(TLSProperties tlsProperties) {
        this.tlsProperties = tlsProperties;
    }
}
