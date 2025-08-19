# ------------ Stage 1: build phase ------------
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ------------ Stage 2: runtime image ------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# copy the jar built in stage 1
COPY --from=build /app/target/NIMASA-NYSC-Clearance-Form-0.0.1-SNAPSHOT.jar.original app.jar

# Ensure port passed by Render is used
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=$PORT"]
