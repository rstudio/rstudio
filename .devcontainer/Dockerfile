FROM ubuntu:focal

ARG USERNAME=rstudio
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN apt-get update && \
    export DEBIAN_FRONTEND=noninteractive && \
    apt-get install -y \
    ant \
    build-essential \
    clang \
    cmake \
    curl \
    debsigs \
    docker.io \
    dpkg-sig \
    expect \
    fakeroot \
    gdebi-core \
    git \
    gnupg1 \
    iproute2 \
    libacl1-dev \
    libattr1-dev \
    libbz2-dev \
    libcap-dev \
    libclang-6.0-dev \
    libclang-dev \
    libcurl4-openssl-dev \
    libegl1-mesa \
    libfuse2 \
    libgl1-mesa-dev \
    libgtk-3-0 \
    libnss3 \
    libpam-dev \
    libpango1.0-dev \
    libpq-dev \
    libsqlite3-dev \
    libssl-dev \
    libuser1-dev \
    libxml2-dev \
    libxslt1-dev \
    locales \
    lsof \
    ninja-build \
    p7zip-full \
    patchelf \
    pkg-config \
    python \
    python3 \
    python3-venv \
    r-base \
    rrdtool \
    software-properties-common \
    sudo \
    unzip \
    uuid \
    uuid-dev \
    valgrind \
    vim \
    wget \
    xvfb \
    zlib1g-dev

RUN localedef -i en_US -f UTF-8 en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

# Java 8 and 11 (not in official repo for focal)
RUN add-apt-repository -y ppa:openjdk-r/ppa && \
    apt-get update && \
    apt-get install -y openjdk-8-jdk openjdk-11-jdk
RUN update-alternatives --set java /usr/lib/jvm/java-8-openjdk-$(dpkg --print-architecture)/jre/bin/java

# copy common dependency installation scripts
RUN mkdir -p /opt/rstudio-tools/dependencies/linux
RUN mkdir -p /opt/rstudio-tools/dependencies/tools
RUN mkdir -p /opt/rstudio-tools/dependencies/common
COPY ./dependencies/tools/rstudio-tools.sh /opt/rstudio-tools/dependencies/tools/

# copy panmirror project setup (so install-common can install needed deps)
RUN mkdir -p /opt/rstudio-tools/panmirror
COPY ./src/gwt/panmirror/src/editor/yarn.lock /opt/rstudio-tools/panmirror/
COPY ./src/gwt/panmirror/src/editor/package.json /opt/rstudio-tools/panmirror/

# install common dependencies
# here, we deviate from the typical "./install-common" script and instead
# call the individual dependency scripts for better layer caching.

# crashpad
COPY ./dependencies/common/install-crashpad /opt/rstudio-tools/dependencies/common
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-crashpad bionic

# dictionaries
COPY ./dependencies/common/install-dictionaries /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-dictionaries

# mathjax
COPY ./dependencies/common/install-mathjax /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-mathjax

# boost
COPY ./dependencies/common/install-boost /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-boost

# pandoc
COPY ./dependencies/common/install-pandoc /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-pandoc

# R packages
COPY ./dependencies/common/install-packages /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-packages

# sentry
COPY ./dependencies/common/install-sentry-cli /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-sentry-cli

# NPM dependencies
COPY ./dependencies/common/install-npm-dependencies /opt/rstudio-tools/dependencies/common/
COPY ./dependencies/common/install-node /opt/rstudio-tools/dependencies/common/
COPY ./dependencies/common/install-yarn /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-npm-dependencies

# soci
COPY ./dependencies/common/install-soci /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-soci

# yaml-cpp
COPY ./dependencies/common/install-yaml-cpp /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-yaml-cpp

# Create the user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME \
    && echo "$USERNAME:$USERNAME" | sudo chpasswd

# Create config folder. The postCreateCommand in devcontainer.json will link the config
RUN mkdir /etc/rstudio

# Create the secure cookie key
RUN sh -c "echo `uuid` > /etc/rstudio/secure-cookie-key"
RUN chmod 0600 /etc/rstudio/secure-cookie-key

# Install AWS CLI v2
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws

# Install docker-compose
RUN curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
RUN chmod +x /usr/local/bin/docker-compose

USER ${USERNAME}
