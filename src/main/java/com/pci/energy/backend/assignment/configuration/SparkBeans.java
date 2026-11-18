package com.pci.energy.backend.assignment.configuration;

import org.apache.spark.sql.SparkSession;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AssignmentProperties.class)
public class SparkBeans {


    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession(AssignmentProperties props) {
        System.setProperty("HADOOP_USER_NAME", props.hadoop().user());

        String catalog = props.iceberg().catalogName();
        String catalogPrefix = "spark.sql.catalog." + catalog;

        return SparkSession.builder()
                .appName(props.spark().appName())
                .master(props.spark().master())
                // Extensiones SQL de Iceberg (MERGE INTO, Time Travel, CALL procedures)
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
                .config("spark.jars.packages", "org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.12.0")
                // Configuración del catálogo Hadoop en HDFS

                .config("spark.sql.defaultCatalog", catalog)
                .config(catalogPrefix, "org.apache.iceberg.spark.SparkCatalog")
                .config(catalogPrefix + ".type", "hadoop")
                .config("spark.executor.maxNumFailures","5")

                .config("spark.driver.host", "backend")
                .config("spark.driver.bindAddress", "0.0.0.0")
                .config("spark.driver.port", "50501")
                .config("spark.driver.blockManager.port", "50502")

                .config(catalogPrefix + ".warehouse", props.iceberg().warehouse())
                // Configuración del cliente HDFS
                .config("spark.hadoop.fs.defaultFS", props.hadoop().defaultFs())
                .config("spark.hadoop.dfs.client.use.datanode.hostname", "true")
                .config("spark.sql.session.timeZone", "UTC")
                .getOrCreate();
    }

}
