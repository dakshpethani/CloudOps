
package com.cloudops.cloudops_api.repository;

import com.cloudops.cloudops_api.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByAlertKeyAndActiveTrue(String alertKey);

    List<Alert> findAllByOrderByCreatedAtDesc();
    long countByActiveTrue();
}
