FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY target/discovery-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]