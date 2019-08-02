FROM debian:stretch

# update apt repository to cloudfront's mirror
RUN set -x \
    && sed -i "s/deb.debian.org/cloudfront.debian.net/" /etc/apt/sources.list \
    && sed -i "s/security.debian.org/cloudfront.debian.net/" /etc/apt/sources.list

# update system
RUN set -x \
    && apt-get update -y

# install necessary packages
RUN apt-get update -y && apt-get install -y \
    ant \
    libboost-all-dev \
    bzip2 \
    clang-4.0 \
    curl \
    debsigs \
    dpkg-sig \
    expect \
    fakeroot \
    gcc \
    git \
    gnupg1 \
    openjdk-8-jdk  \
    libacl1-dev \
    libcap-dev \
    libcurl4-openssl-dev \
    libpam0g-dev \
    libffi-dev \
    procps \
    uuid-dev \
    make \
    libssl1.0-dev \
    libpango-1.0-0 \
    r-base \
    rrdtool \
    sudo \
    wget \
    libxml-commons-external-java \
    mesa-common-dev \
    zlib1g \
    libattr1-dev \
    libcap-dev \
    libacl1-dev \
    lsof \
    python \
    libuser1-dev \
    libglib2.0-dev \
    valgrind

## run install-boost twice - boost exits 1 even though it has installed good enough for our uses.
## https://github.com/rstudio/rstudio/blob/master/vagrant/provision-primary-user.sh#L12-L15
COPY dependencies/common/install-boost /tmp/
RUN bash /tmp/install-boost || bash /tmp/install-boost

# install cmake
COPY package/linux/install-dependencies /tmp/
RUN bash /tmp/install-dependencies

# add clang to system path
ENV PATH=/usr/lib/llvm-4.0/bin:$PATH

# install crashpad and its dependencies
COPY dependencies/common/install-crashpad /tmp/
RUN bash /tmp/install-crashpad

# set github login from build argument if defined
ARG GITHUB_LOGIN
ENV RSTUDIO_GITHUB_LOGIN=$GITHUB_LOGIN

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
