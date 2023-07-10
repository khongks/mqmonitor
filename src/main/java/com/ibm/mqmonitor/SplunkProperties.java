package com.ibm.mqmonitor;

import java.io.Serializable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.micrometer.common.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "splunk")
public class SplunkProperties implements Serializable {
    
    private String token;
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    private String endpoint;
    public String getEndpoint() {
        return endpoint;
    }
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    @Override
    public String toString() {
        return "SplunkProperties [token=" + maskString(token, 30) + ", endpoint=" + endpoint + "]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
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
        SplunkProperties other = (SplunkProperties) obj;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        if (endpoint == null) {
            if (other.endpoint != null)
                return false;
        } else if (!endpoint.equals(other.endpoint))
            return false;
        return true;
    }

    private String maskString(String s, int x) {
        int n = s.length()/x;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (n >= 1 && (i < n || i >= (s.length() - n))) {
                sb.append(s.charAt(i));
            }
            else {
                sb.append("*");
            }
        }
        return sb.toString();
    }
}
