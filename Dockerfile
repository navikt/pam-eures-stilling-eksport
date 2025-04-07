FROM gcr.io/distroless/java21

COPY target/pam-eures-stilling-eksport*.jar app.jar
ENV JAVA_OPTS="-XX:-OmitStackTraceInFastThrow"
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
