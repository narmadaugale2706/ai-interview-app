# Use Java 17
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy project files
COPY . .

# Build the project
RUN ./gradlew build

# Run the jar
CMD ["java", "-jar", "build/libs/*.jar"]