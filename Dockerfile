FROM eclipse-temurin:21-jdk-alpine AS build
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/api-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
CMD ["sh","-c","java -XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=70  -XshowSettings $JAVA_OPTS -jar api-0.0.1-SNAPSHOT.jar"]
