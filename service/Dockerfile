FROM maven:3.9.14-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
ADD https://github.com/aws-observability/aws-otel-java-instrumentation/releases/download/v2.26.1/aws-opentelemetry-agent.jar /opt/aws-opentelemetry-agent.jar

RUN chmod 0444 /opt/aws-opentelemetry-agent.jar \
    && groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=build /workspace/target/service-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
