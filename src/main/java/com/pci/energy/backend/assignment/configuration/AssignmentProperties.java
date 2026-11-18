package com.pci.energy.backend.assignment.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assignment")
public record AssignmentProperties(
        SparkProps spark,
        HadoopProps hadoop,
        IcebergProps iceberg
) {
    public record SparkProps(String appName, String master) {
    }

    public record HadoopProps(String defaultFs, String user) {
    }

    public record IcebergProps(String catalogName, String warehouse, String defaultNamespace) {
    }
}