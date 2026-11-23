FROM eclipse-temurin:21-jdk AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build --chown=10001:10001 /build/target/plataforma-reservas-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:10001

ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=\"${PORT:-8080}\" -jar /app/app.jar"]
