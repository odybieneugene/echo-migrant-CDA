# --- Étape de build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Étape d'exécution ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p /app/uploads/images
COPY --from=build /app/target/echo-migrant-backend.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
