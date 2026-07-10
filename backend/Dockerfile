# --- Étape 1 : build avec Maven + JDK 17 ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Le pom puis le code source, et on package (sans les tests pour un déploiement rapide).
# Maven télécharge les dépendances pendant le package : pas besoin de go-offline
# (étape connue pour échouer sur certaines configs).
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# --- Étape 2 : image d'exécution légère (JRE seulement) ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Le jar construit à l'étape précédente
COPY --from=build /app/target/*.jar app.jar

# Bride la JVM pour tenir dans une petite instance (ex. Railway 0,5 Go / Render free).
# MaxRAMPercentage=70 => le heap prend ~70% de la RAM du conteneur, le reste
# (metaspace, threads, off-heap) rentre dans les 30% restants. Evite les OOM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

# Railway/Render injectent la variable PORT ; Spring l'utilise via application.yml
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
