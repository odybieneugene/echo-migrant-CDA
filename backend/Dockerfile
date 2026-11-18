# --- Étape de build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Dépendances d'abord : cette couche reste en cache tant que pom.xml ne change pas
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# Les tests tournent en CI (GitHub Actions) ; l'image ne fait que packager
RUN mvn -B clean package -DskipTests

# --- Étape d'exécution (JRE seul, sans Maven ni sources) ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Utilisateur sans privilèges : une faille dans l'application ne donne pas les droits root du conteneur
RUN groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /app/uploads/images && chown -R app:app /app
COPY --from=build --chown=app:app /app/target/echo-migrant-backend.jar app.jar
USER app
EXPOSE 8081
# Limite la mémoire de la JVM à 75 % de celle allouée au conteneur
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
