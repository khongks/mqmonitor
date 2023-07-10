package com.ibm.mqmonitor;

import java.io.Serializable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mq")
public class MQProperties implements Serializable {

    private String queueManagerName;
    public String getQueueManagerName() {
        return queueManagerName;
    }
    public void setQueueManagerName(String queueManagerName) {
        this.queueManagerName = queueManagerName;
    }
    private String hostname;
    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    private Integer port;
    public Integer getPort() {
        return port;
    }
    public void setPort(Integer port) {
        this.port = port;
    }
    private String channelName;
    public String getChannelName() {
        return channelName;
    }
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
    private String user;
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    private String genericQueueName;
    public String getGenericQueueName() {
        return genericQueueName;
    }
    public void setGenericQueueName(String genericQueueName) {
        this.genericQueueName = genericQueueName;
    }
    @Override
    public String toString() {
        return "MQProperties [queueManagerName=" + queueManagerName + ", hostname=" + hostname + ", port=" + port
                + ", channelName=" + channelName + ", user=" + user + ", password=" + password + ", genericQueueName="
                + genericQueueName + "]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((queueManagerName == null) ? 0 : queueManagerName.hashCode());
        result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        result = prime * result + ((channelName == null) ? 0 : channelName.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((genericQueueName == null) ? 0 : genericQueueName.hashCode());
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
        MQProperties other = (MQProperties) obj;
        if (queueManagerName == null) {
            if (other.queueManagerName != null)
                return false;
        } else if (!queueManagerName.equals(other.queueManagerName))
            return false;
        if (hostname == null) {
            if (other.hostname != null)
                return false;
        } else if (!hostname.equals(other.hostname))
            return false;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        if (channelName == null) {
            if (other.channelName != null)
                return false;
        } else if (!channelName.equals(other.channelName))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (genericQueueName == null) {
            if (other.genericQueueName != null)
                return false;
        } else if (!genericQueueName.equals(other.genericQueueName))
            return false;
        return true;
    }
}
