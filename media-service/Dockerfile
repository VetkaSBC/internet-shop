FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY target/*-SNAPSHOT.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]