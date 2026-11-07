
package com.cloudops.cloudops_api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "servers")
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    private String host;
    private String serverType;
    private Integer exporterPort;
    private String status;

    private double cpu;
    private double memory;
    private Integer servicePort;

    // Email address that receives alerts for this server
    private String alertEmail;

    public Server() {
    }

    public Server(String name, String host) {
        this.name = name;
        this.host = host;
        this.status = "UNKNOWN";
        this.cpu = 0;
        this.memory = 0;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getServerType() {
        return serverType;
    }

    public Integer getExporterPort() {
        return exporterPort;
    }

    public String getStatus() {
        return status;
    }

    public double getCpu() {
        return cpu;
    }

    public double getMemory() {
        return memory;
    }

    public Integer getServicePort() {
        return servicePort;
    }

    public String getAlertEmail() {
        return alertEmail;
    }

    public void setAlertEmail(String alertEmail) {
        this.alertEmail = alertEmail;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public void setExporterPort(Integer exporterPort) {
        this.exporterPort = exporterPort;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCpu(double cpu) {
        this.cpu = cpu;
    }

    public void setMemory(double memory) {
        this.memory = memory;
    }

    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }
}
