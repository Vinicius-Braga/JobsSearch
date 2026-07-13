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

RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /app/build/libs/*.jar app.jar
COPY empresas.txt ./
RUN chown -R app:app /app

USER app

EXPOSE 8080
# Contêiner sem rota IPv6 funcional (comum no Docker Desktop) faz chamadas HTTPS
# externas (Gupy, API da Claude) falharem com "Network is unreachable" se a JVM
# tentar IPv6 primeiro — força IPv4.
# MaxRAMPercentage=75: o padrão da JVM (25%) é conservador demais quando o app é o
# único processo no container (ex: plano free do Render, com pouca RAM disponível).
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
