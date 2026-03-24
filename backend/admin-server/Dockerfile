FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/admin-server-1.0.0.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
