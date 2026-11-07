package com.cloudops.cloudops_api.repository;

import com.cloudops.cloudops_api.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    Optional<Server> findFirstByHostAndServerTypeAndServicePort(
            String host,
            String serverType,
            Integer servicePort
    );


    Optional<Server> findFirstByHostAndServerTypeAndExporterPort(String host, String type, Integer port);
}