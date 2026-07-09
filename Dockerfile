# --- build ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# --- runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY empresas.txt filtro.txt ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
