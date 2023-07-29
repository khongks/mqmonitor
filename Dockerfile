# FROM registry.access.redhat.com/ubi9/openjdk-17:1.15-1.1686736679
FROM registry.connect.redhat.com/ibm/ibm-semeru-runtime-open-17-jre-ubi:latest

## Install debugging tools
USER root
# RUN microdnf install nc openssl -y
RUN dnf install nc openssl -y

ARG JAR_FILE=target/mqmonitor-1.0.0.jar
COPY ${JAR_FILE} /app.jar
# ENTRYPOINT ["java","-Djavax.net.debug=ssl:handshake","-Dcom.ibm.mq.cfg.useIBMCipherMappings=false","-jar","/app.jar"]
ENTRYPOINT ["java","-Dcom.ibm.mq.cfg.useIBMCipherMappings=false","-jar","/app.jar"]