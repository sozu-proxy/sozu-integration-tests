FROM jenkins/jnlp-slave:alpine

USER root

RUN apk update && apk add -u libcurl curl

ARG DOCKER_VERSION=18.03.0-ce

RUN curl -fsSL https://download.docker.com/linux/static/stable/`uname -m`/docker-$DOCKER_VERSION.tgz | tar --strip-components=1 -xz -C /usr/local/bin docker/docker