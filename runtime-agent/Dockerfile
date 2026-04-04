FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY webstore-app/pom.xml /workspace/webstore-app/pom.xml
COPY runtime-agent/pom.xml /workspace/runtime-agent/pom.xml
RUN mvn -q -pl runtime-agent -am -DskipTests dependency:go-offline

COPY runtime-agent/src/ /workspace/runtime-agent/src/
RUN mvn -q -pl runtime-agent -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/runtime-agent/target/runtime-agent-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
