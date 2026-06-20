# ─────────────────────────────────────────────────────────────────────
#  Étape 1 — build Maven (image avec JDK + Maven)
# ─────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache des dépendances : on copie d'abord le pom seul.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ─────────────────────────────────────────────────────────────────────
#  Étape 2 — runtime léger (JRE seul)
# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/racines-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
