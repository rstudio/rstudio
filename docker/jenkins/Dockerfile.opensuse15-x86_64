FROM opensuse/leap:15.0

# needed to build RPMs
RUN zypper --non-interactive addrepo http://download.opensuse.org/repositories/systemsmanagement:wbem:deps/openSUSE_Tumbleweed/systemsmanagement:wbem:deps.repo

# refresh repos and install required packages
RUN zypper --non-interactive --gpg-auto-import-keys refresh && \
    zypper --non-interactive install -y \
    ant \
    boost-devel \
    clang \
    curl \
    expect \
    fakeroot \
    gcc \
    gcc-c++ \
    git \
    java-1_8_0-openjdk-devel  \
    libacl-devel \
    libattr-devel \
    libcap-devel \
    libcurl-devel \
    libuser-devel \
    libuuid-devel \
    libXcursor-devel \
    libXrandr-devel \
    lsof \
    make \
    openssl-devel \
    pam-devel \
    pango-devel \
    python \
    python-xml \
    R \
    rpm-build \
    sudo \
    tar \
    unzip \
    valgrind \
    wget \
    xml-commons-apis \
    zlib-devel

## run install-boost twice - boost exits 1 even though it has installed good enough for our uses.
## https://github.com/rstudio/rstudio/blob/master/vagrant/provision-primary-user.sh#L12-L15
COPY dependencies/common/install-boost /tmp/
RUN bash /tmp/install-boost || bash /tmp/install-boost

# install cmake
COPY package/linux/install-dependencies /tmp/
RUN bash /tmp/install-dependencies

# install crashpad and its dependencies
COPY dependencies/common/install-crashpad /tmp/
RUN bash /tmp/install-crashpad

# set github login from build argument if defined
ARG GITHUB_LOGIN
ENV RSTUDIO_GITHUB_LOGIN=$GITHUB_LOGIN

# ensure we use the java 8 compiler
RUN update-alternatives --set java /usr/lib64/jvm/jre-1.8.0-openjdk/bin/java

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

# install GnuPG 1.4 from source (needed for release signing)
RUN cd /tmp && \
    wget https://gnupg.org/ftp/gcrypt/gnupg/gnupg-1.4.23.tar.bz2 && \
    bzip2 -d gnupg-1.4.23.tar.bz2 && \
    tar xvf gnupg-1.4.23.tar && \
    cd gnupg-1.4.23 && \
    ./configure && \
    make && \
    make install

# create jenkins user, make sudo. try to keep this toward the bottom for less cache busting
ARG JENKINS_GID=999
ARG JENKINS_UID=999
RUN groupadd -g $JENKINS_GID jenkins && \
    useradd -m -d /var/lib/jenkins -u $JENKINS_UID -g jenkins jenkins && \
    echo "jenkins ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
