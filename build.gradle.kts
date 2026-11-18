import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.launcher.daemon.protocol.Build
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.pci.energy.backend"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

val sparkVersion = "3.5.4"
val icebergVersion = "1.12.0"
val hadoopVersion = "3.4.1"
val scalaBinaryVersion = "2.12"

configurations.configureEach {
    // Evita conflictos de logging entre Spark/Hadoop y Spring Boot (Logback)
    exclude(group = "org.slf4j", module = "slf4j-reload4j")
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl")
    exclude(group = "org.apache.logging.log4j", module = "log4j-1.2-api")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet-core:2.41")
    implementation("jakarta.servlet:jakarta.servlet-api:4.0.4")

    // Apache Spark Core & SQL
    implementation("org.apache.spark:spark-core_$scalaBinaryVersion:$sparkVersion")
    implementation("org.apache.spark:spark-sql_$scalaBinaryVersion:$sparkVersion")


    // Apache Iceberg Runtime para Spark 3.5
    implementation("org.apache.iceberg:iceberg-spark-runtime-3.5_$scalaBinaryVersion:$icebergVersion")

    // Cliente Hadp HDFS
    implementation("org.apache.hadoop:hadoop-client-api:$hadoopVersion")
    implementation("org.apache.hadoop:hadoop-client-runtime:$hadoopVersion")

    // Alineación de Jackson Scala con el BOM de Spring Boot
    implementation("com.fasterxml.jackson.module:jackson-module-scala_$scalaBinaryVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
}

// Flags JVM requeridos por Spark en Java 21
val sparkJvmArgs = listOf(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-opens=java.base/java.net=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
    "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
    "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED"
)

tasks.named<BootRun>("bootRun") {
    jvmArgs = sparkJvmArgs
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs = sparkJvmArgs
}