FROM openjdk:17-alpine

WORKDIR /app

# glibc 설치
RUN wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.31-r0/glibc-2.31-r0.apk && \
    apk add --no-cache glibc-2.31-r0.apk && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.31-r0/glibc-bin-2.31-r0.apk && \
    apk add --no-cache glibc-bin-2.31-r0.apk && \
    /usr/glibc-compat/sbin/ldconfig /lib /usr/glibc-compat/lib

# 필수 라이브러리 설치
RUN apk add --no-cache libstdc++ zlib

# AWS CLI 설치 (x86_64 버전)
RUN apk add --no-cache bash curl zip unzip && \
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws

# kubectl 설치
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/

ARG JAR_PATH=./build/libs

COPY ${JAR_PATH}/Zolang-server-0.0.1-SNAPSHOT.jar ./app.jar

RUN mkdir -p resources/images resources/logs

ENV LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib

CMD ["java", "-jar", "./app.jar", "--spring.profiles.active=dev"]
