FROM ubuntu:24.04

SHELL ["/bin/bash", "-c"]

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=UTC

# Base tools
RUN apt-get update && apt-get install -y \
    curl wget git git-lfs gnupg lsb-release ca-certificates \
    unzip zip jq htop vim nano less sudo \
    openssh-client iputils-ping netcat-openbsd dnsutils \
    && rm -rf /var/lib/apt/lists/*

# Java 21 (Eclipse Temurin)
RUN curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
    | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" \
    > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && apt-get install -y temurin-21-jdk && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/temurin-21-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Maven 3.9
ARG MAVEN_VERSION=3.9.9
RUN curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    | tar -xz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

# Node.js 22
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

# Bun (for eterna-docs)
RUN curl -fsSL https://bun.sh/install | bash
ENV BUN_INSTALL=/root/.bun
ENV PATH=$BUN_INSTALL/bin:$PATH

# just (task runner)
RUN curl -sSfL https://just.systems/install.sh | bash -s -- --to /usr/local/bin

# Docker CLI + Compose plugin (socket from host)
RUN install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    chmod a+r /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y docker-ce-cli docker-compose-plugin && \
    rm -rf /var/lib/apt/lists/*

# Create vscode user (devcontainer standard)
RUN useradd -m -s /bin/bash -G sudo vscode && \
    echo "vscode ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/vscode

# Give vscode access to Docker socket (GID 999 is Docker's default)
RUN groupadd -g 999 docker 2>/dev/null || true && \
    usermod -aG docker vscode

# Bun + env for vscode user
RUN curl -fsSL https://bun.sh/install | BUN_INSTALL=/home/vscode/.bun bash && \
    echo 'export BUN_INSTALL=$HOME/.bun' >> /home/vscode/.bashrc && \
    echo 'export PATH=$BUN_INSTALL/bin:$PATH' >> /home/vscode/.bashrc && \
    echo 'export JAVA_HOME=/usr/lib/jvm/temurin-21-amd64' >> /home/vscode/.bashrc && \
    echo 'export PATH=$JAVA_HOME/bin:/opt/maven/bin:$PATH' >> /home/vscode/.bashrc && \
    chown -R vscode:vscode /home/vscode/.bun

USER vscode
WORKDIR /workspaces/eterna-stack
