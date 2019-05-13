FROM registry.pinsvc.net/mirror/openjdk:8-jre-alpine

RUN apk add --no-cache bash curl tzdata

RUN rm /etc/localtime
RUN ln -s /usr/share/zoneinfo/Asia/Tehran /etc/localtime
RUN echo 'Asia/Tehran' > /etc/timezone

EXPOSE 9000

MAINTAINER Vahid Nourhani

ADD target/universal/market-service-1.0.tgz /pintapin/backend/market-service

WORKDIR /pintapin/backend/market-service