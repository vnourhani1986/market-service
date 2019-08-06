FROM registry.pinsvc.net/mirror/openjdk:8-jre-alpine

RUN apk add --no-cache bash curl tzdata

ENV TZ Asia/Tehran

EXPOSE 9000

MAINTAINER Vahid Nourhani

ADD target/universal/market-service-1.0.tgz /pintapin/backend/market-service

WORKDIR /pintapin/backend/market-service