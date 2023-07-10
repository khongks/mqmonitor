FROM registry.access.redhat.com/ubi9/openjdk-17:1.15-1.1686736679

ARG JAR_FILE=target/mqmonitor-1.0.0.jar
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]