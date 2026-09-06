FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/zhida-ai-0.0.1-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=ollama
ENV ZHIDA_STORAGE_PATH=/app/data
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xmx768m", "-jar", "/app/app.jar"]
