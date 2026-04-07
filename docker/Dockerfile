# SUSE BCI base with JRE 21
FROM registry.suse.com/bci/bci-base:latest
LABEL maintainer="info@whitered.se" vendor="WHITE RED CONSULTING AB"

# Install JRE 21 and system dependencies
RUN set -ex; \
    zypper -n refresh && \
    zypper -n update && \
    zypper -n install --no-recommends \
        java-21-openjdk-headless \
        clamav \
        curl \
        jq \
        rsync && \
    zypper clean -a

COPY ./docker/docker-files/clamd.conf /etc/clamav/clamd.conf

# Copy the Roda WUI JAR file
COPY roda-ui/roda-wui/target/roda-wui-*.jar  /KEEPS/bin/

ENV RODA_HOME=/roda \
    LDAP_SERVER_URL=ldap://openldap \
    LDAP_SERVER_PORT=1389 \
    SIEGFRIED_MODE=server \
    SIEGFRIED_SERVER_URL=http://siegfried:5138 \
    SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/roda_core_db \
    RODA_USER="roda" \
    RODA_UID="1000" \
    RODA_GROUP="roda" \
    RODA_GID="1000"

COPY ./docker/docker-files/docker-entrypoint.sh /
COPY ./docker/docker-files/docker-entrypoint.d/* /docker-entrypoint.d/

RUN set -ex; \
    groupadd -g "$RODA_GID" "$RODA_GROUP" && \
    useradd -u "$RODA_UID" -g "$RODA_GID" -m -d /home/roda "$RODA_USER" && \
    mkdir -p -m0770 "$RODA_HOME/data" && \
    mkdir -p -m0770 "$RODA_HOME/config" && \
    chown -R "$RODA_USER:0" "$RODA_HOME"

VOLUME "$RODA_HOME/data"

WORKDIR "$RODA_HOME"

USER "$RODA_UID"

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
  CMD curl -f http://localhost:8080/api/openapi.json || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
