package com.pci.energy.backend.assignment;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.VolumesFrom;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

public class BaseIntegrationTests implements AutoCloseable {

    public static final int DRIVER_RPC_PORT = 50501;
    public static final int DRIVER_BLOCK_MANAGER_PORT = 50502;
    public static final String ICEBERG_RUNTIME = "org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.12.0";
    private static final DockerImageName HADOOP_IMAGE = DockerImageName.parse("apache/hadoop:3.4.1");
    private static final DockerImageName SPARK_IMAGE = DockerImageName.parse("apache/spark:3.5.4-java17-python3");
    private final Network network = Network.newNetwork();
    private final GenericContainer<?> namenode;
    private final GenericContainer<?> datanode;
    private final GenericContainer<?> sparkMaster;
    private final GenericContainer<?> sparkWorker;

    public BaseIntegrationTests() {
        namenode = new GenericContainer<>(HADOOP_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("namenode")
                .withExposedPorts(9870, 9000)
                .withEnv("CORE-SITE.XML_fs.defaultFS", "hdfs://namenode:9000")
                .withEnv("HDFS-SITE.XML_dfs.replication", "1")
                .withEnv("HDFS-SITE.XML_dfs.permissions.enabled", "false")
                .withEnv("HDFS-SITE.XML_dfs.namenode.datanode.registration.ip-hostname-check", "false")
                .withEnv("HDFS-SITE.XML_dfs.client.use.datanode.hostname", "true")
                .withEnv("HDFS-SITE.XML_dfs.datanode.use.datanode.hostname", "true")
                .withCommand("bash", "-c", "if [ ! -d /tmp/hadoop-root/dfs/name/current ]; then hdfs namenode -format -force; fi; exec hdfs namenode")
                .waitingFor(Wait.forHttp("/").forPort(9870).withStartupTimeout(Duration.ofMinutes(2)));

        datanode = new GenericContainer<>(HADOOP_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("datanode")
                .withExposedPorts(9864, 9866, 50010)
                .withEnv("CORE-SITE.XML_fs.defaultFS", "hdfs://namenode:9000")
                .withEnv("HDFS-SITE.XML_dfs.replication", "1")
                .withEnv("HDFS-SITE.XML_dfs.permissions.enabled", "false")
                .withEnv("HDFS-SITE.XML_dfs.namenode.datanode.registration.ip-hostname-check", "false")
                .withEnv("HDFS-SITE.XML_dfs.client.use.datanode.hostname", "true")
                .withEnv("HDFS-SITE.XML_dfs.datanode.use.datanode.hostname", "true")
                .withEnv("HDFS-SITE.XML_dfs.datanode.hostname", "datanode")
                .withCommand("hdfs", "datanode")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        sparkMaster = new GenericContainer<>(SPARK_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("spark-master")
                .withExposedPorts(8080, 7077)
                .withCommand("bash", "-lc", sparkMasterCommand())
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        sparkWorker = new GenericContainer<>(SPARK_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("spark-worker")
                .withExposedPorts(8081)
                .withEnv("SPARK_WORKER_CORES", "2")
                .withEnv("SPARK_WORKER_MEMORY", "2g")
                .withCommand("bash", "-lc", sparkWorkerCommand())
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    }

    /**
     * Start dependencies in order, then initialize HDFS and the Spark standalone cluster.
     */
    public static BaseIntegrationTests start() throws IOException, InterruptedException {
        // Lets containers reach the Spark driver running in the host JVM.
        Testcontainers.exposeHostPorts(DRIVER_RPC_PORT, DRIVER_BLOCK_MANAGER_PORT);

        BaseIntegrationTests env = new BaseIntegrationTests();
        try {
            env.namenode.start();
            env.datanode.start();
            env.initializeHdfs();
            env.sparkMaster.start();
            env.sparkWorker.start();
            return env;
        } catch (RuntimeException | IOException | InterruptedException e) {
            env.close();
            throw e;
        }
    }

    private void initializeHdfs() throws IOException, InterruptedException {
        var result = namenode.execInContainer(
                "bash", "-lc",
                "hdfs dfsadmin -safemode wait && " +
                        "hdfs dfs -mkdir -p /warehouse && " +
                        "hdfs dfs -chmod -R 777 /warehouse");
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("HDFS initialization failed: " + result.getStderr());
        }
    }

    /**
     * Spark master URL reachable from the host JVM running the integration test.
     */
    public String sparkMasterUrl() {
        return "spark://" + sparkMaster.getHost() + ":" + sparkMaster.getMappedPort(7077);
    }

    /**
     * NameNode address through Docker Desktop's host gateway, usable from host and containers.
     */
    public String hdfsUri() {
        return "hdfs:/" + namenode.getContainerName() + ":" + namenode.getMappedPort(9000);
    }

    public String sparkUiUrl() {
        return "http://" + sparkMaster.getHost() + ":" + sparkMaster.getMappedPort(8080);
    }

    /**
     * Apply the client-mode settings needed by executors to call back to the host driver.
     */
    public SparkSession.Builder configure(SparkSession.Builder builder) {
        return builder
                .master(sparkMasterUrl())
                .config("spark.driver.host", "host.testcontainers.internal")
                .config("spark.driver.bindAddress", "0.0.0.0")
                .config("spark.driver.port", DRIVER_RPC_PORT)
                .config("spark.driver.blockManager.port", DRIVER_BLOCK_MANAGER_PORT)
                .config("spark.jars.packages", ICEBERG_RUNTIME)
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
                .config("spark.sql.defaultCatalog", "iceberg")
                .config("spark.sql.catalog.iceberg", "org.apache.iceberg.spark.SparkCatalog")
                .config("spark.sql.catalog.iceberg.type", "hadoop")
                .config("spark.sql.catalog.iceberg.warehouse", hdfsUri() + "/warehouse")
                .config("spark.hadoop.fs.defaultFS", hdfsUri());
    }

    private String sparkMasterCommand() {
        return "wget -P '/opt/spark/jars/' 'https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-3.5_2.12/1.12.0/iceberg-spark-runtime-3.5_2.12-1.12.0.jar'" +
                "mkdir -p /opt/spark/conf /root/.ivy2 && " +
                "printf '%s\\n' " +
                "'spark.hadoop.fs.defaultFS hdfs://" + this.namenode.getContainerName() + ":9000' " +
                "'spark.sql.extensions org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions' " +
                "'spark.sql.defaultCatalog iceberg' " +
                "'spark.sql.catalog.iceberg org.apache.iceberg.spark.SparkCatalog' " +
                "'spark.sql.catalog.iceberg.type hadoop' " +
                "'spark.sql.catalog.iceberg.warehouse hdfs://" + this.namenode.getContainerName() + ":9000/warehouse' " +
                "> /opt/spark/conf/spark-defaults.conf && " +
                "exec /opt/spark/bin/spark-class org.apache.spark.deploy.master.Master " +
                "--host spark-master --port 7077 --webui-port 8080";
    }

    private String sparkWorkerCommand() {
        return "wget -P '/opt/spark/jars/' 'https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-3.5_2.12/1.12.0/iceberg-spark-runtime-3.5_2.12-1.12.0.jar'" +
                "mkdir -p /opt/spark/conf /root/.ivy2 && " +
                "printf '%s\\n' " +
                "'spark.hadoop.fs.defaultFS hdfs://" + this.namenode.getContainerName() + ":9000' " +
                "'spark.sql.extensions org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions' " +
                "'spark.sql.defaultCatalog iceberg' " +
                "'spark.sql.catalog.iceberg org.apache.iceberg.spark.SparkCatalog' " +
                "'spark.sql.catalog.iceberg.type hadoop' " +
                "'spark.sql.catalog.iceberg.warehouse hdfs://" + this.namenode.getContainerName() + ":9000/warehouse' " +
                "> /opt/spark/conf/spark-defaults.conf && " +
                "exec /opt/spark/bin/spark-class org.apache.spark.deploy.worker.Worker " +
                "spark://spark-master:7077 --webui-port 8081";
    }

    @Override
    public void close() {
        stop(sparkWorker);
        stop(sparkMaster);
        stop(datanode);
        stop(namenode);
        network.close();
    }

    private static void stop(GenericContainer<?> container) {
        try {
            container.stop();
        } catch (Exception ignored) {
            // Continue stopping the remaining containers during cleanup.
        }
    }
}