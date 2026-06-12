FROM ubuntu:24.04
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends libreoffice netcat-openbsd python3-pip && \
    pip3 install --no-cache-dir --break-system-packages unoserver==3.7 && \
    apt-get clean && rm -rf /var/lib/apt/lists/* && \
    fc-cache -f && \
    useradd -r -u 1001 -m unoserver && \
    mkdir -p /home/unoserver/.cache/dconf && \
    chown -R unoserver:unoserver /home/unoserver
USER unoserver
ENV UNOSERVER_CONVERSION_TIMEOUT=360
ENV FONTCONFIG_CACHE=/home/unoserver/.cache/fontconfig
CMD ["sh", "-c", "unoserver --interface 0.0.0.0 --port 2003 --conversion-timeout ${UNOSERVER_CONVERSION_TIMEOUT}"]
