FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package \
    && test -f target/tech-kernel-backend-0.0.1-SNAPSHOT.jar

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/tech-kernel-backend-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001

EXPOSE 8080 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
