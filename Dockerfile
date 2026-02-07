FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /workspace
COPY fileUploader/pom.xml fileUploader/pom.xml
COPY worker-service/pom.xml worker-service/pom.xml
COPY fileUploader/src fileUploader/src
COPY worker-service/src worker-service/src

RUN mvn -f fileUploader/pom.xml -DskipTests package
RUN mvn -f worker-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /opt/app
COPY --from=builder /workspace/fileUploader/target/fileUploader-0.0.1-SNAPSHOT.jar /opt/app/fileUploader.jar
COPY --from=builder /workspace/worker-service/target/worker-service-0.0.1-SNAPSHOT.jar /opt/app/worker-service.jar
COPY docker/entrypoint.sh /opt/app/entrypoint.sh

RUN chmod +x /opt/app/entrypoint.sh

EXPOSE 8080 8081

ENTRYPOINT ["/opt/app/entrypoint.sh"]