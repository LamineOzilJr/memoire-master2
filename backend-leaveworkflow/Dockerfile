# Stage 1: Build the app
FROM maven:3.8.6-amazoncorretto-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=build /app/target/leaveworkflow-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]