FROM ubuntu:24.04
RUN apt-get update && \
    apt-get install -y --no-install-recommends libreoffice netcat-openbsd python3-pip && \
    pip3 install --no-cache-dir --break-system-packages unoserver==3.3.2 && \
    apt-get clean && rm -rf /var/lib/apt/lists/* && \
    useradd -r -u 1001 unoserver
USER unoserver
ENV UNOSERVER_CONVERSION_TIMEOUT=360
CMD ["sh", "-c", "unoserver --interface 0.0.0.0 --port 2003 --conversion-timeout ${UNOSERVER_CONVERSION_TIMEOUT}"]
