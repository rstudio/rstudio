FROM ubuntu:bionic

ARG AWS_REGION=us-east-1

# install needed packages. replace httpredir apt source with cloudfront
RUN set -x \
    && sed -i "s/archive.ubuntu.com/$AWS_REGION.ec2.archive.ubuntu.com/" /etc/apt/sources.list \
    && export DEBIAN_FRONTEND=noninteractive \
    && apt-get update \
    && apt-get install -y gnupg1 \
    && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 0x51716619e084dab9 \
    && echo 'deb http://cran.rstudio.com/bin/linux/ubuntu bionic-cran35/' >> /etc/apt/sources.list \
    && apt-get update

# add ppa repository so we can install java 8 (not in any official repo for bionic)
RUN apt-get update \
  && apt-get install -y software-properties-common \
  && add-apt-repository ppa:openjdk-r/ppa

RUN apt-get update && \
    export DEBIAN_FRONTEND=noninteractive && \
    apt-get install -y \
    ant \
    build-essential \
    clang \
    curl \
    debsigs \
    dpkg-sig \
    expect \
    fakeroot \
    git-core \
    libattr1-dev \
    libacl1-dev \
    libbz2-dev \
    libcap-dev \
    libcurl4-openssl-dev \
    libfuse2 \
    libgtk-3-0 \
    libgl1-mesa-dev \
    libegl1-mesa \
    libpam-dev \
    libpango1.0-dev \
    libuser1-dev \
    libssl-dev \
    libxslt1-dev \
    lsof \
    openjdk-8-jdk \
    pkg-config \
    python \
    r-base \
    sudo \
    unzip \
    uuid-dev \
    valgrind \
    wget \
    zlib1g-dev

# ensure we use the java 8 compiler
RUN update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

## build patchelf
RUN cd /tmp \
    && wget https://nixos.org/releases/patchelf/patchelf-0.9/patchelf-0.9.tar.gz \
    && tar xzvf patchelf-0.9.tar.gz \
    && cd patchelf-0.9 \
    && ./configure \
    && make \
    && make install

## run install-boost twice - boost exits 1 even though it has installed good enough for our uses.
## https://github.com/rstudio/rstudio/blob/master/vagrant/provision-primary-user.sh#L12-L15
COPY dependencies/common/install-boost /tmp/
RUN bash /tmp/install-boost || bash /tmp/install-boost

# set github login from build argument if defined
ARG GITHUB_LOGIN
ENV RSTUDIO_GITHUB_LOGIN=$GITHUB_LOGIN

# install cmake
COPY package/linux/install-dependencies /tmp/
RUN /bin/bash /tmp/install-dependencies

# install common dependencies
RUN mkdir -p /opt/rstudio-tools/dependencies/common
COPY dependencies/common/* /opt/rstudio-tools/dependencies/common/
RUN cd /opt/rstudio-tools/dependencies/common && /bin/bash ./install-common

# install Qt SDK
COPY dependencies/linux/install-qt-sdk /tmp/
RUN mkdir -p /opt/RStudio-QtSDK && \
    export QT_SDK_DIR=/opt/RStudio-QtSDK/Qt5.12.1 && \
    export QT_QPA_PLATFORM=minimal && \
    /tmp/install-qt-sdk

# create jenkins user, make sudo. try to keep this toward the bottom for less cache busting
ARG JENKINS_GID=999
ARG JENKINS_UID=999
RUN groupadd -g $JENKINS_GID jenkins && \
    useradd -m -d /var/lib/jenkins -u $JENKINS_UID -g jenkins jenkins && \
    echo "jenkins ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

