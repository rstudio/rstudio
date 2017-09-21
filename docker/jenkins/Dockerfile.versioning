FROM alpine:3.6
RUN apk -v --update add \
		bash \
        python \
        py-pip \
	    git \
        groff \
        less \
        mailcap \
        sudo \
        && \
     pip install --upgrade awscli s3cmd python-magic && \
     apk -v --purge del py-pip && \
     rm /var/cache/apk/* && \
     mkdir -p /scripts

ARG JENKINS_GID=999
ARG JENKINS_UID=999
COPY docker/jenkins/*.sh /tmp/
RUN /tmp/clean-uid.sh $JENKINS_UID && \
    /tmp/clean-gid.sh $JENKINS_GID

RUN addgroup -g $JENKINS_GID jenkins && \
    adduser -D -h /var/lib/jenkins -u $JENKINS_UID -G jenkins jenkins && \
    echo "jenkins ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
