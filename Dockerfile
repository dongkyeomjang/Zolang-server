FROM ubuntu:20.04

WORKDIR /app

# DEBIAN_FRONTEND 설정을 통해 tzdata 설정 비활성화
ENV DEBIAN_FRONTEND=noninteractive

# 필수 패키지 설치
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk wget unzip libstdc++6 zlib1g docker.io curl git bash zip maven nodejs npm

# 최신 Gradle 설치
RUN wget https://services.gradle.org/distributions/gradle-8.0.2-bin.zip && \
    unzip gradle-8.0.2-bin.zip && \
    mv gradle-8.0.2 /opt/gradle && \
    ln -s /opt/gradle/bin/gradle /usr/bin/gradle && \
    chmod +x /opt/gradle/bin/gradle

# Yarn 설치
RUN npm install -g yarn

# 도커 데몬 소켓을 볼륨으로 연결
VOLUME /var/run/docker.sock

# AWS CLI 설치 (x86_64 버전)
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws

# kubectl 설치
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/

# Buildx 설치
RUN mkdir -p ~/.docker/cli-plugins/ && \
    curl -SL https://github.com/docker/buildx/releases/download/v0.6.3/buildx-v0.6.3.linux-amd64 -o ~/.docker/cli-plugins/docker-buildx && \
    chmod +x ~/.docker/cli-plugins/docker-buildx && \
    docker buildx create --use --name mybuilder && \
    docker buildx inspect --bootstrap

# Helm 설치
RUN curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

ARG JAR_PATH=./build/libs

COPY ${JAR_PATH}/Zolang-server-0.0.1-SNAPSHOT.jar ./app.jar

RUN mkdir -p resources/repo resources/logs

ENV LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib

CMD ["java", "-jar", "./app.jar", "--spring.profiles.active=dev"]