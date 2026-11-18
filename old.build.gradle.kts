import sun.jvmstat.monitor.MonitoredVmUtil.jvmArgs

plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}
configurations {
    all {
        exclude("org.springframework.boot", "spring-boot-starter-logging")
        exclude("org.apache.logging.log4j", "log4j-to-slf4j")
        exclude("log4j", "log4j")
        exclude("org.slf4j", "slf4j-reload4j")
        exclude("ch.qos.reload4j", "slf4j-reload4j")
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("javax.servlet", "javax.servlet-api")
        //org.apache.logging.log4j/log4j-slf4j2-impl/2.25.5/3b675c42c5e9de05ed363e3bfddcbd7d71e8c5b0/log4j-slf4j2-impl-
    }
}

group = "com.pci.energy.backend"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
val springDataHadoopVersion = "2.3.0.RELEASE"
val sparkVersion = "3.5.5"
val icebergVersion = "1.8.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    //implementation("org.springframework.data:spring-data-hadoop-spark:${springDataHadoopVersion}")
    //implementation("org.apache.spark:spark-yarn_2.10:${sparkVersion}")
    implementation("org.apache.spark:spark-core_2.12:${sparkVersion}")
    implementation("org.apache.spark:spark-sql_2.12:${sparkVersion}")
    // TODO: migrate to spark connect
    // implementation("org.apache.spark:spark-connect_2.12:${sparkVersion}")
    //implementation("org.apache.spark:spark-connect-client-jvm_2.12:${sparkVersion}")
    //implementation("org.springframework.data:spring-data-hadoop-boot:${springDataHadoopVersion}")
    //implementation("org.springframework.data:spring-data-hadoop-batch:${springDataHadoopVersion}")
    //implementation("org.springframework.data:spring-data-hadoop-spark:${springDataHadoopVersion}")
    //implementation("org.apache.spark:spark-yarn_2.10:${sparkVersion}")
    //implementation("org.slf4j:slf4j-api:1.6.4")
    implementation("javax.servlet:javax.servlet-api:4.0.1")
    //implementation("org.slf4j:slf4j-log4j12:1.6.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:${icebergVersion}")
    implementation("org.apache.iceberg:iceberg-core:${icebergVersion}")
    implementation("org.apache.hadoop:hadoop-common:3.4.0")
    implementation("org.apache.parquet:parquet-hadoop:1.12.2")
    //implementation("org.springframework.boot:spring-boot-starter-log4j2")
    //implementation("org.springframework.boot:spring-boot-starter-batch")
    //implementation("org.slf4j:slf4j-simple:1.7.36")
    //implementation("io.netty:netty-all:4.1.17.Final")
    //implementation("com.globalmentor:hadoop-bare-naked-local-fs:0.1.0")
    //implementation("org.apache.logging.log4j:log4j-api:2.25.2")
    //implementation("org.apache.logging.log4j:log4j-core:2.25.2")
    //implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs = listOf("-Xdoclint:none", "-Xlint:none", "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
}