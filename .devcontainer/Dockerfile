FROM ubuntu:focal

ARG USERNAME=rstudio
ARG USER_UID=1000
ARG USER_GID=$USER_UID

# install preliminaries not covered by the install-dependencies-focal script
RUN apt-get update && \
    export DEBIAN_FRONTEND=noninteractive && \
    apt-get install -y \
    curl \
    gdebi-core \
    git \
    libxml2-dev \
    locales \
    p7zip-full \
    sudo \
    valgrind 

RUN localedef -i en_US -f UTF-8 en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

# Create the user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

USER ${USERNAME}
