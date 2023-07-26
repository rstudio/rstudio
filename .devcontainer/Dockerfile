ARG OS_ARCH=jammy-x86_64
ARG BRANCH_NAME=mountain-hydrangea
ARG IMAGE_PREFIX
FROM 263245908434.dkr.ecr.us-east-1.amazonaws.com/jenkins/ide:${IMAGE_PREFIX}${OS_ARCH}-${BRANCH_NAME}

ARG USERNAME=rstudio
ARG USER_UID=1000
ARG USER_GID=$USER_UID
ARG AWS_ARCH=x86_64

# Create the user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME \
    && echo "$USERNAME:$USERNAME" | sudo chpasswd

# Create config folder. The postCreateCommand in devcontainer.json will link the config
RUN mkdir /etc/rstudio

# If AWS_ARCH is x86_64 and we are on an Ubuntu distribution (check by grepping lsb-release), install gcc-multilib
RUN if [ "$AWS_ARCH" = "x86_64" ] && [ -n "$(grep 'Ubuntu' /etc/lsb-release)" ]; then \
    apt-get update && \
    apt-get install -y gcc-multilib; \
    fi

# Create the secure cookie key
RUN sh -c "echo `uuid` > /etc/rstudio/secure-cookie-key"
RUN chmod 0600 /etc/rstudio/secure-cookie-key

# Install AWS CLI v2
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-${AWS_ARCH}.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws

# Create user home directory
RUN mkdir -p /home/${USERNAME} && chown ${USER_UID}:${USER_GID} /home/${USERNAME}

USER ${USERNAME}

# Run the init-dev.sh script
COPY --chown=${USERNAME}:${USERNAME} init-dev.sh /home/${USERNAME}/init-dev.sh
RUN /home/${USERNAME}/init-dev.sh
