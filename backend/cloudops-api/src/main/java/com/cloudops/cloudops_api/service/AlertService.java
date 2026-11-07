
package com.cloudops.cloudops_api.service;

import com.cloudops.cloudops_api.entity.Alert;
import com.cloudops.cloudops_api.entity.Server;
import com.cloudops.cloudops_api.repository.AlertRepository;
import com.cloudops.cloudops_api.repository.ServerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final ServerRepository serverRepository;
    private final EmailNotificationService emailNotificationService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public AlertService(
            AlertRepository alertRepository,
            ServerRepository serverRepository,
            EmailNotificationService emailNotificationService
    ) {
        this.alertRepository = alertRepository;
        this.serverRepository = serverRepository;
        this.emailNotificationService = emailNotificationService;
    }

    // Get alert history
    public List<Alert> getAlertHistory() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    // Create, update, or resolve alerts
    @Transactional
    public synchronized void updateAlert(
            String alertKey,
            Long serverId,
            String serverName,
            String message,
            String severity,
            boolean currentlyTriggered
    ) {

        List<Alert> existingAlerts =
                alertRepository.findAllByAlertKeyAndActiveTrue(alertKey);

        // NEW ALERT
        if (currentlyTriggered) {

            if (existingAlerts.isEmpty()) {

                Alert alert = new Alert();

                alert.setAlertKey(alertKey);
                alert.setServerId(serverId);
                alert.setServerName(serverName);
                alert.setMessage(message);
                alert.setSeverity(severity);
                alert.setCreatedAt(LocalDateTime.now());
                alert.setActive(true);

                alertRepository.save(alert);

                String subject =
                        "[CloudOps] " + severity + " Alert - " + serverName;

                String emailBody = buildAlertEmail(alert);

                sendEmailAfterCommit(
                        serverId,
                        subject,
                        emailBody
                );

            } else {

                Alert mainAlert = existingAlerts.get(0);

                mainAlert.setMessage(message);
                mainAlert.setSeverity(severity);

                alertRepository.save(mainAlert);

                // Remove duplicate active alerts
                for (int i = 1; i < existingAlerts.size(); i++) {

                    Alert duplicate = existingAlerts.get(i);
                    duplicate.setActive(false);

                    alertRepository.save(duplicate);
                }
            }

        } else {

            // RESOLVE ACTIVE ALERTS
            for (Alert alert : existingAlerts) {

                alert.setActive(false);
                alertRepository.save(alert);
            }

            // Send recovery notification
            if (!existingAlerts.isEmpty()) {

                Alert alert = existingAlerts.get(0);

                String subject =
                        "[CloudOps] RECOVERED - " + alert.getServerName();

                String emailBody = buildRecoveryEmail(alert);

                sendEmailAfterCommit(
                        alert.getServerId(),
                        subject,
                        emailBody
                );
            }
        }
    }

    // Build professional new-alert email
    private String buildAlertEmail(Alert alert) {

        String metric = alert.getMessage() == null
                ? "Infrastructure Metric"
                : alert.getMessage();

        String severity = alert.getSeverity() == null
                ? "WARNING"
                : alert.getSeverity().toUpperCase();

        String description;
        String recommendations;

        String lowerMessage = metric.toLowerCase();

        if (lowerMessage.contains("cpu")) {

            description =
                    "CPU utilization has exceeded its configured threshold. "
                            + "High CPU consumption may cause slow application response, "
                            + "increased processing time, or service instability.";

            recommendations =
                    "1. Identify processes consuming high CPU resources.\n"
                            + "2. Check application and system processes.\n"
                            + "3. Investigate unusual CPU usage patterns.\n"
                            + "4. Optimize workloads or increase CPU capacity if required.";

        } else if (lowerMessage.contains("memory")
                || lowerMessage.contains("ram")) {

            description =
                    "Memory utilization has exceeded its configured threshold. "
                            + "High memory consumption may affect application performance "
                            + "and could lead to memory exhaustion.";

            recommendations =
                    "1. Identify memory-intensive processes.\n"
                            + "2. Check application memory consumption.\n"
                            + "3. Investigate possible memory leaks.\n"
                            + "4. Optimize memory usage or increase available RAM if required.";

        } else if (lowerMessage.contains("disk")) {

            description =
                    "Disk utilization has exceeded its configured threshold. "
                            + "Insufficient disk space may affect application operations, "
                            + "logging, and database performance.";

            recommendations =
                    "1. Check available disk space.\n"
                            + "2. Identify large files and unnecessary logs.\n"
                            + "3. Review database and application storage usage.\n"
                            + "4. Free up space or expand storage if required.";

        } else {

            description =
                    "CloudOps has detected an infrastructure condition "
                            + "that requires administrator attention.";

            recommendations =
                    "1. Open the CloudOps Monitoring Dashboard.\n"
                            + "2. Review the affected server and metric.\n"
                            + "3. Investigate the underlying issue.\n"
                            + "4. Take corrective action and monitor the server.";
        }

        return
                "==================================================\n"
                        + "          CLOUDOPS MONITORING SYSTEM\n"
                        + "       INFRASTRUCTURE ALERT NOTIFICATION\n"
                        + "==================================================\n\n"

                        + "Hello Administrator,\n\n"

                        + "CloudOps has detected an infrastructure issue "
                        + "that requires your attention.\n\n"

                        + "---------------- ALERT DETAILS ----------------\n\n"

                        + "Server Name : " + alert.getServerName() + "\n"
                        + "Server ID   : " + alert.getServerId() + "\n"
                        + "Alert Type  : " + alert.getAlertKey() + "\n"
                        + "Metric      : " + metric + "\n"
                        + "Severity    : " + severity + "\n"
                        + "Status      : ACTIVE\n"
                        + "Detected At : " + alert.getCreatedAt().format(DATE_FORMAT) + "\n\n"

                        + "---------------- DESCRIPTION ------------------\n\n"

                        + description + "\n\n"

                        + "---------------- RECOMMENDED ACTIONS -----------\n\n"

                        + recommendations + "\n\n"

                        + "---------------- IMPORTANT ---------------------\n\n"

                        + "Please investigate this issue through your "
                        + "CloudOps Monitoring Dashboard.\n"
                        + "Continuous resource consumption beyond safe "
                        + "limits may affect server availability.\n\n"

                        + "==================================================\n"
                        + "CloudOps | Infrastructure Monitoring & Alert Management\n"
                        + "This is an automated notification. Please do not reply.\n"
                        + "==================================================";
    }

    // Build professional recovery email
    private String buildRecoveryEmail(Alert alert) {

        String metric = alert.getMessage() == null
                ? "Infrastructure Metric"
                : alert.getMessage();

        return
                "==================================================\n"
                        + "          CLOUDOPS MONITORING SYSTEM\n"
                        + "          ALERT RECOVERY NOTIFICATION\n"
                        + "==================================================\n\n"

                        + "Hello Administrator,\n\n"

                        + "Good news! CloudOps has detected that the "
                        + "previously triggered alert condition is no longer active.\n\n"

                        + "---------------- RECOVERY DETAILS ---------------\n\n"

                        + "Server Name : " + alert.getServerName() + "\n"
                        + "Server ID   : " + alert.getServerId() + "\n"
                        + "Alert Type  : " + alert.getAlertKey() + "\n"
                        + "Metric      : " + metric + "\n"
                        + "Severity    : " + alert.getSeverity() + "\n"
                        + "Status      : RESOLVED\n"
                        + "Resolved At : " + LocalDateTime.now().format(DATE_FORMAT) + "\n\n"

                        + "---------------- DESCRIPTION ------------------\n\n"

                        + "The monitoring condition that previously triggered "
                        + "this alert is no longer being detected.\n\n"

                        + "The server metric has returned below its configured "
                        + "alert threshold. Please continue monitoring the "
                        + "server to ensure stable operation.\n\n"

                        + "---------------- NEXT STEPS --------------------\n\n"

                        + "1. Verify that the server is operating normally.\n"
                        + "2. Review recent monitoring history if required.\n"
                        + "3. Continue monitoring resource utilization.\n\n"

                        + "==================================================\n"
                        + "CloudOps | Infrastructure Monitoring & Alert Management\n"
                        + "This is an automated recovery notification. Please do not reply.\n"
                        + "==================================================";
    }

    // Send email after database transaction commits
    private void sendEmailAfterCommit(
            Long serverId,
            String subject,
            String message
    ) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        try {

                            Server server = serverRepository.findById(serverId)
                                    .orElse(null);

                            if (server == null) {
                                System.err.println(
                                        "Email skipped: Server not found. ID: "
                                                + serverId
                                );
                                return;
                            }

                            String recipient = server.getAlertEmail();

                            if (recipient == null || recipient.isBlank()) {

                                System.err.println(
                                        "Email skipped: Alert email not configured for server: "
                                                + server.getName()
                                );

                                return;
                            }

                            emailNotificationService.sendAlertEmail(
                                    recipient,
                                    subject,
                                    message
                            );

                            System.out.println(
                                    "Alert email sent to: " + recipient
                            );

                        } catch (Exception e) {

                            System.err.println(
                                    "Email notification failed: "
                                            + e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    // Count active alerts
    public long getActiveAlertCount() {
        return alertRepository.countByActiveTrue();
    }
}
