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
    openjdk-8-jdk \
    p7zip-full \
    patchelf \
    pkg-config \
    python \
    r-base \
    rrdtool \
    software-properties-common \
    sudo \
    unzip \
    uuid-dev \
    valgrind \
    wget \
    xvfb \
    zlib1g-dev

RUN localedef -i en_US -f UTF-8 en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

# Java 8 (not in official repo for focal)
RUN add-apt-repository -y ppa:openjdk-r/ppa && apt-get update && apt-get -y install openjdk-8-jdk
RUN update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

# Create the user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

USER ${USERNAME}
