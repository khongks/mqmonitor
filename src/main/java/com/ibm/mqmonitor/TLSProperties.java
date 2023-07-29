package com.ibm.mqmonitor;

import java.io.Serializable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mq.tls")
public class TLSProperties implements Serializable {

    private String mode;
    private Boolean debug;
    private String cipherSuite;
    private KeystoreProperties keystoreProperties;
    private TruststoreProperties truststoreProperties;

    public TLSProperties(KeystoreProperties keystoreProperties, TruststoreProperties truststoreProperties ) {
        this.keystoreProperties = keystoreProperties;
        this.truststoreProperties = truststoreProperties;
    }

    public String getMode() {
        return mode;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getCipherSuite() {
        return cipherSuite;
    }

    public void setCipherSuite(String cipherSuite) {
        this.cipherSuite = cipherSuite;
    }

    public KeystoreProperties getKeystoreProperties() {
        return keystoreProperties;
    }

    public void setKeystoreProperties(KeystoreProperties keystoreProperties) {
        this.keystoreProperties = keystoreProperties;
    }

    public TruststoreProperties getTruststoreProperties() {
        return truststoreProperties;
    }
    public void setTruststoreProperties(TruststoreProperties truststoreProperties) {
        this.truststoreProperties = truststoreProperties;
    }

    @Override
    public String toString() {
        return "TLSProperties [mode=" + mode + ", debug=" + debug + ", cipherSuite=" + cipherSuite
                + ", keystoreProperties=" + keystoreProperties + ", truststoreProperties=" + truststoreProperties + "]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + ((debug == null) ? 0 : debug.hashCode());
        result = prime * result + ((cipherSuite == null) ? 0 : cipherSuite.hashCode());
        result = prime * result + ((keystoreProperties == null) ? 0 : keystoreProperties.hashCode());
        result = prime * result + ((truststoreProperties == null) ? 0 : truststoreProperties.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TLSProperties other = (TLSProperties) obj;
        if (mode == null) {
            if (other.mode != null)
                return false;
        } else if (!mode.equals(other.mode))
            return false;
        if (debug == null) {
            if (other.debug != null)
                return false;
        } else if (!debug.equals(other.debug))
            return false;
        if (cipherSuite == null) {
            if (other.cipherSuite != null)
                return false;
        } else if (!cipherSuite.equals(other.cipherSuite))
            return false;
        if (keystoreProperties == null) {
            if (other.keystoreProperties != null)
                return false;
        } else if (!keystoreProperties.equals(other.keystoreProperties))
            return false;
        if (truststoreProperties == null) {
            if (other.truststoreProperties != null)
                return false;
        } else if (!truststoreProperties.equals(other.truststoreProperties))
            return false;
        return true;
    }
}
