FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# ✅ IMPORTANT FIX
RUN chmod +x gradlew

# Build project
RUN ./gradlew build

# Run jar
CMD ["java", "-jar", "build/libs/*.jar"]