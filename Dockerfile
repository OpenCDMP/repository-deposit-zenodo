####################################### Build stage #######################################
FROM maven:3.9-eclipse-temurin-21-alpine AS build-stage

ARG REVISION
ARG PROFILE

COPY pom.xml /build/
COPY core /build/core/
COPY web /build/web/
COPY settings.xml /root/.m2/settings.xml
RUN rm -f /build/web/src/main/resources/config/app.env
RUN rm -f /build/web/src/main/resources/config/*-dev.yml

WORKDIR /build/
RUN mvn -Drevision=${REVISION} -P${PROFILE} dependency:go-offline
RUN mvn -Drevision=${REVISION} -P${PROFILE} clean package
######################################## Run Stage ########################################
FROM eclipse-temurin:21-jre-alpine

ARG PROFILE
ARG REVISION
ENV SERVER_PORT=8080
EXPOSE ${SERVER_PORT}

COPY --from=build-stage /build/web/target/repository-deposit-web-${REVISION}.jar /app/repository-deposit-web.jar

ENTRYPOINT ["java","-Dspring.config.additional-location=file:/config/","-Dspring.profiles.active=${PROFILE}","-Djava.security.egd=file:/dev/./urandom","-jar","/app/repository-deposit-web.jar"]