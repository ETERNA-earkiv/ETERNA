FROM registry.suse.com/bci/bci-base:15.7
LABEL maintainer="info@whitered.se" vendor="WHITE RED CONSULTING AB"

RUN set -ex; \
    zypper -n refresh && \
    zypper -n update && \
    zypper -n install --no-recommends \
        java-21-openjdk-headless \
        clamav \
        rsync \
        jq \
        curl \
        glibc-locale-base && \
    zypper clean -a && \
    chmod u-s /usr/bin/su && \
    chmod u-s /usr/bin/newgrp && \
    chmod u-s /usr/bin/newuidmap && \
    chmod u-s /usr/bin/newgidmap && \
    chmod u-s /usr/bin/passwd && \
    chmod u-s /usr/bin/gpasswd && \
    chmod u-s /usr/bin/mount && \
    chmod u-s /usr/bin/umount && \
    chmod u-s /usr/bin/chfn && \
    chmod u-s /usr/bin/chsh && \
    chmod g-s /usr/bin/expiry && \
    chmod g-s /usr/bin/chage && \
    chmod g-s /sbin/unix_chkpwd && \
    chmod g-s /sbin/unix2_chkpwd && \
    chmod g-s /usr/lib/utempter/utempter

COPY docker-files/clamd.conf /etc/clamd.conf

COPY target/roda-wui-*.jar /WhiteRed/bin/

ENV LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8 \
    RODA_HOME=/roda \
    LDAP_SERVER_URL=ldap://openldap \
    LDAP_SERVER_PORT=1389 \
    SIEGFRIED_MODE=server \
    SIEGFRIED_SERVER_URL=http://siegfried:5138 \
    SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/roda_core_db \
    RODA_USER="roda" \
    RODA_UID="1000" \
    RODA_GROUP="roda" \
    RODA_GID="1000" \
    UNOSERVER_HOST=unoserver \
    UNOSERVER_PORT=2003

COPY docker-files/docker-entrypoint.sh /
COPY docker-files/docker-entrypoint.d/* /docker-entrypoint.d/

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
  CMD curl -f http://localhost:8080/api/v2/openapi || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
