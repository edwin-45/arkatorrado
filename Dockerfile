# Usa una imagen base ligera de Java
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY build/libs/arkatorrado-0.0.1-SNAPSHOT.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]