FROM ghcr.io/navikt/baseimages/temurin:21

ENV JAVA_OPTS="-XX:-OmitStackTraceInFastThrow"

COPY target/pam-eures-stilling-eksport*.jar app.jar
EXPOSE 8080
