FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} consul-vertx.jar
ENV porta=2233
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom -Dporta=$porta","-jar","/consul-vertx.jar"]