FROM ghcr.io/navikt/baseimages/temurin:17
LABEL maintainer="Team Jakt"

ENV JAVA_OPTS="-XX:-OmitStackTraceInFastThrow"

COPY target/pam-eures-stilling-eksport*.jar app.jar
EXPOSE 8080
