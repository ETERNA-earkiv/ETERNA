# Stage 1: Pull Tomcat binaries from the official image
FROM tomcat:10.1-jre21 AS tomcat

# Stage 2: SUSE BCI base with JRE 21
FROM registry.suse.com/bci/bci-base:latest
LABEL maintainer="info@whitered.se" vendor="WHITE RED CONSULTING AB"

# Install JRE 21 and system dependencies
RUN set -ex; \
    zypper -n refresh && \
    zypper -n update && \
    zypper -n install --no-recommends \
        java-21-openjdk-headless \
        clamav \
        zip \
        unzip \
        rsync \
        jq && \
    zypper clean -a

# Copy Tomcat from the official image (maintains /usr/local/tomcat layout)
ENV CATALINA_HOME=/usr/local/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH
COPY --from=tomcat /usr/local/tomcat $CATALINA_HOME

# Point Tomcat to the BCI JRE instead of the Temurin JRE from the source image
RUN set -ex; \
    JAVA_BIN=$(readlink -f /usr/bin/java) && \
    echo "export JAVA_HOME=${JAVA_BIN%/bin/java}" > "$CATALINA_HOME/bin/setenv.sh"

COPY ./docker-files/clamd.conf /etc/clamav/clamd.conf

# Remove default ROOT webapp
RUN set -ex; \
    rm -rf /usr/local/tomcat/webapps/ROOT

# Install web application
COPY ./target/  /usr/local/tomcat/webapps/ROOT

# Fix configuration & do some final cleanup
RUN mkdir -p /usr/local/tomcat/conf/Catalina/localhost
RUN chmod -R a+rwx /usr/local/tomcat/conf/Catalina

RUN set -ex; \
    unzip -q /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/roda-core-*.jar config/roda-core.properties && \
    sed -i -e 's/^core.plugins.internal.virus_check.clamav/#&/' -e 's/^core.tools.siegfried.mode/#&/' config/roda-core.properties && \
    printf '\n' >> config/roda-core.properties && \
    echo "core.plugins.internal.virus_check.clamav.bin = /usr/bin/clamdscan" >> config/roda-core.properties && \
    echo "core.plugins.internal.virus_check.clamav.params = -m --fdpass" >> config/roda-core.properties && \
    echo "core.plugins.internal.virus_check.clamav.get_version = clamdscan --version" >> config/roda-core.properties && \
    echo "core.tools.siegfried.mode = server" >> config/roda-core.properties && \
    zip -q /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/roda-core-*.jar config/roda-core.properties && \
    zypper clean -a

ENV RODA_HOME=/roda \
    SIEGFRIED_MODE=server \
    SIEGFRIED_SERVER_URL=http://siegfried:5138 \
    RODA_USER="roda" \
    RODA_UID="1000" \
    RODA_GROUP="roda" \
    RODA_GID="1000"

COPY ./docker-files/docker-entrypoint.sh /
COPY ./docker-files/docker-entrypoint.d/* /docker-entrypoint.d/

RUN set -ex; \
    groupadd -g "$RODA_GID" "$RODA_GROUP" && \
    useradd -u "$RODA_UID" -g "$RODA_GID" -m -d /home/roda "$RODA_USER" && \
    mkdir -p -m0770 "$RODA_HOME/data" && \
    mkdir -p -m0770 "$RODA_HOME/config" && \
    chown -R "$RODA_USER:0" "$RODA_HOME"

VOLUME "$RODA_HOME/data"

WORKDIR $RODA_HOME

USER "$RODA_UID"

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
  CMD curl -f http://localhost:8080/api/openapi.json || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
