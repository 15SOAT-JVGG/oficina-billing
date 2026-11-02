# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.12-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache de dependências: copia apenas os descritores de build primeiro
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN mvn dependency:go-offline -B -q

# Copia o código-fonte e empacota. Os testes e os gates rodam no CI (./mvnw verify);
# aqui só se produz o artefato.
COPY src/ src/
RUN mvn package -B -DskipTests

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24

ENV LANGUAGE='en_US:en'

COPY --from=builder --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=builder --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=builder --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

# A imagem não carrega chave alguma: a chave pública RS256 do OS Service chega em tempo
# de execução por JWT_PUBLIC_KEY_LOCATION (Secret no Kubernetes, volume no Compose).

EXPOSE 8080 9000
USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT ["/opt/jboss/container/java/run/run-java.sh"]
