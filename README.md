# CloudOps – Infrastructure Monitoring & Alert Management Platform

CloudOps is a full-stack infrastructure monitoring and alert management platform designed to monitor server health, track resource utilization, detect performance issues, and notify administrators when critical infrastructure events occur.

The platform provides a centralized dashboard for monitoring CPU, memory, and disk usage, managing servers, viewing monitoring metrics, and tracking alerts in real time.

## 🚀 Key Features

- **Centralized Dashboard:** View server status, infrastructure health, and monitoring summaries from one place.
- **Server Management:** Maintain and monitor registered servers through a unified interface.
- **Resource Monitoring:** Track CPU, memory, and disk utilization.
- **Prometheus Integration:** Collect and monitor infrastructure metrics using Prometheus.
- **Multi-Environment Monitoring:** Support monitoring for Windows, Tomcat, and Node.js environments.
- **Alert Management:** Detect resource threshold violations and maintain alert history.
- **Email Notifications:** Notify administrators when alerts are triggered or resolved.
- **Alert Lifecycle Management:** Track active, resolved, and historical alerts.
- **Persistent Storage:** Store server and alert information in MySQL.
- **Modern Web Interface:** Responsive dark-themed dashboard built with React.

## 🏗️ System Architecture

CloudOps follows a modular architecture consisting of a React frontend, Spring Boot backend, monitoring agents, Prometheus, and MySQL.

```text
                 +----------------------+
                 |     React Frontend   |
                 |    Dashboard / UI    |
                 +----------+-----------+
                            |
                            | REST API
                            |
                 +----------v-----------+
                 |   Spring Boot API    |
                 | Server & Alert Mgmt |
                 +----+------------+----+
                      |            |
                 +----v----+  +----v---------+
                 |  MySQL  |  | Alert Service|
                 |Database |  +------+-------+
                 +---------+         |
                                     v
                              Email Notification
                                     |
                                     v
                               Administrator

       +-------------------------------------------+
       |              Prometheus                   |
       |           Metrics Collection              |
       +---------------------+---------------------+
                             |
               +-------------+-------------+
               |             |             |
          Java Agent   Windows Exporter  Other
           Metrics        Metrics       Exporters
```

## 🛠️ Technology Stack

| Category | Technologies |
|---|---|
| Frontend | React.js, JavaScript, HTML, CSS |
| Backend | Java, Spring Boot |
| Database | MySQL 8.0 |
| Monitoring | Prometheus |
| Monitoring Exporters | Java Agent, Windows Exporter, JMX Exporter |
| Build Tool | Maven |
| API Communication | REST APIs |
| Version Control | Git, GitHub |
| Development Tools | IntelliJ IDEA, VS Code |



## 📊 Monitoring Capabilities

CloudOps provides visibility into important infrastructure metrics:

| Metric | Description |
|---|---|
| CPU Usage | Tracks processor utilization |
| Memory Usage | Monitors memory consumption |
| Disk Usage | Tracks disk space utilization |
| Server Status | Displays server health and availability |
| Alert History | Maintains records of triggered alerts |

## 🔔 Alert Management

The alert management module continuously evaluates monitoring data against configured thresholds.

When a threshold is exceeded:

1. The monitoring system identifies the resource issue.
2. An alert is generated and stored.
3. The administrator receives an email notification.
4. The alert is displayed in the Alerts dashboard.
5. When the issue is resolved, the alert lifecycle is updated and a recovery notification can be sent.


## 🔐 Security

- Sensitive configuration files and credentials are excluded from version control.
- Environment variables are used for database and email credentials.
- Gmail SMTP authentication uses an application password rather than the regular account password.

## 🎯 Project Objectives

CloudOps was developed to explore practical implementation of:

- Full-stack application development
- Backend API development using Spring Boot
- Infrastructure monitoring and observability
- Prometheus-based metrics collection
- Database integration using MySQL
- Automated alerting and email notifications
- Cloud and DevOps engineering concepts

## 👨‍💻 Author

**Daksh Pethani**

- B.Tech Information Technology
- Dharmsinh Desai University

Interested in Cloud Engineering, Backend Development, AWS, and DevOps.

- GitHub: [@dakshpethani](https://github.com/dakshpethani)

---

⭐ If you find this project interesting, consider giving the repository a star!

**CloudOps – Monitor. Analyze. Respond.**
