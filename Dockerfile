FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /workspace/build/libs/bdbd.jar /app/bdbd.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=demo", "-jar", "/app/bdbd.jar"]
