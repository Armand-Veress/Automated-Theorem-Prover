FROM maven:3.9-eclipse-temurin-26 AS build

RUN apt-get update && apt-get install -y build-essential

WORKDIR /app

COPY DLX ./DLX
RUN g++ -shared -fPIC -O3 ./DLX/*.cpp -o /app/libdlx_solver.so

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:26-jdk
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/libdlx_solver.so /app/libdlx_solver.so

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.library.path=/app", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]