ARG BASE_IMAGE=public.ecr.aws/amazoncorretto/amazoncorretto:21-al2-jdk
FROM gradle:9.8-jdk21 AS builder
WORKDIR /apps
COPY . .
RUN gradle build -x test --quiet

FROM ${BASE_IMAGE}
COPY --from=builder /apps/build/libs/*.jar /app.jar

ENTRYPOINT [ "sh", "-c", "java  --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.cs=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED -XX:+UseZGC -jar /app.jar " ]
