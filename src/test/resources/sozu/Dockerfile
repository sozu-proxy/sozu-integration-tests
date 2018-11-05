FROM alpine:latest

WORKDIR /source/bin

RUN apk update && apk add --no-cache --virtual .build-dependencies \
  cargo \
  build-base \
  file \
  libgcc \
  musl-dev \
  git \
  rust
RUN apk add --no-cache openssl-dev \
  llvm-libunwind \
  pkgconfig

RUN git clone --single-branch --branch master --depth 1 https://github.com/sozu-proxy/sozu.git .

RUN cargo build --release --all-features --manifest-path bin/Cargo.toml

RUN cargo build --release --all-features --manifest-path ctl/Cargo.toml

RUN echo `ls -l os-build/docker`

RUN mkdir -p /etc/sozu

RUN cp os-build/docker/config.toml /etc/sozu

RUN apk update && apk add --no-cache openssl-dev \
  llvm-libunwind \
  libgcc

RUN cp target/release/sozu /

RUN cp target/release/sozuctl /

RUN echo `ls /`

ENV SOZU_CONFIG /etc/sozu/config.toml

VOLUME /etc/sozu

RUN mkdir -p /run/sozu
VOLUME /run/sozu

EXPOSE 80
EXPOSE 443

ENTRYPOINT ["/sozu"]

CMD ["start", "-c", "/etc/sozu/config.toml"]