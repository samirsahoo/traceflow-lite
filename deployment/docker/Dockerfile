# Built with repo root as context (see docker-compose.yml), so it can COPY
# the already-built jar without duplicating the Maven build inside the image.
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S traceflow && adduser -S traceflow -G traceflow
WORKDIR /app
COPY traceflow-demo/target/traceflow-demo.jar app.jar

USER traceflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
