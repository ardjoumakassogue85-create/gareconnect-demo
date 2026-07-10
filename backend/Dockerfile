# --- Étape 1 : build avec Maven + JDK 17 ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache des dépendances : on copie d'abord le pom
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Puis le code source, et on package (sans les tests pour un déploiement rapide)
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Étape 2 : image d'exécution légère (JRE seulement) ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Le jar construit à l'étape précédente
COPY --from=build /app/target/*.jar app.jar

# Railway/Render injectent la variable PORT ; Spring l'utilise via application.yml
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
