# ==========================================
# Etapa 1: Compilacion con Java 17 y Gradle
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Copiar configuraciones de Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Dar permisos al wrapper y descargar dependencias
RUN chmod +x gradlew

# Copiar codigo fuente y generar el JAR de Spring Boot
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ==========================================
# Etapa 2: Imagen Final Ligera para Render
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario seguro sin privilegios de root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar artefacto compilado
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Render asigna dinamicamente la variable de entorno PORT
ENV PORT=8088
EXPOSE 8088

# Arranque de la aplicacion
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
