# Base images are build arguments, not literals. The defaults below are public i […]
# works on a laptop; a deployed build must pass the enterprise-approved Java 25 […]
# this repository does not know and must not guess.
ARG BUILD_IMAGE=maven:3.9-eclipse-temurin-25
ARG RUNTIME_IMAGE=eclipse-temurin:25-jre

FROM ${BUILD_IMAGE} AS build
WORKDIR /build

# Dependency layer first, so a source-only change does not re-resolve the world.
COPY .mvn/ .mvn/
COPY --chmod=0755 mvnw ./
COPY pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests \
    && java -Djarmode=tools -jar target/automatiam-mcp.jar extract --layers --destination extracted

FROM ${RUNTIME_IMAGE} AS runtime

# A fixed uid, not just USER nonroot: the pod security context pins runAsUser to […]
# and a named user that resolves differently between base images would break that. […]
RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

# Layer order is least- to most-frequently changed, so a code change invalidates […]
COPY --from=build --chown=10001:10001 /build/extracted/dependencies/ ./
COPY --from=build --chown=10001:10001 /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=10001:10001 /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /build/extracted/application/ ./

USER 10001:10001
EXPOSE 8080

# Container-aware heap sizing. A fixed -Xmx would have to be re-tuned every time […]
# limit changes, and the two drift apart silently.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Exec form, so the JVM is pid 1 and receives SIGTERM directly. Without it the s […]
# signal and graceful shutdown never starts; the pod is then killed mid-request […]
ENTRYPOINT ["java", "-jar", "automatiam-mcp.jar"]
