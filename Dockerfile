# syntax=docker/dockerfile:1.7

############################
# 1. Сборка
############################
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Сначала только pom: слой с зависимостями кэшируется, пока pom не меняется
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

COPY src ./src
# Unit/MVC tests run in the build. PostgreSQL integration tests run in CI.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q verify

# Раскладываем jar по слоям (зависимости / loader / snapshot / код приложения):
# при изменении кода пересобирается и пушится только тонкий слой с кодом
RUN java -Djarmode=tools -jar target/app.jar extract --layers --destination /build/extracted

############################
# 2. Рантайм
############################
FROM eclipse-temurin:21-jre-jammy AS runtime

ARG APP_VERSION=dev
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="uzgun-user-service" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_SHA}"

# curl нужен только для HEALTHCHECK; если проверки делает Kubernetes, его можно убрать
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 app \
 && useradd  --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

# Порядок от редко меняющегося к часто меняющемуся
COPY --from=build --chown=10001:10001 /build/extracted/dependencies/          ./
COPY --from=build --chown=10001:10001 /build/extracted/spring-boot-loader/    ./
COPY --from=build --chown=10001:10001 /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /build/extracted/application/           ./

USER 10001:10001

ENV TZ=UTC \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

# exec нужен, чтобы java получила SIGTERM и корректно завершилась (graceful shutdown)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]