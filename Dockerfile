FROM maven:3.8.6-openjdk-8-slim as build

# Setup
WORKDIR /build
MAINTAINER Edgardo Alvarez <ealvarez@allabordo.com>

# Install dependencies
COPY pom.xml .
ENV MAVEN_OPTS "-Dmaven.repo.local=.m2/repository"
RUN mvn clean install

# Build jar build
COPY . .
RUN mvn package -DskipTests

FROM amazoncorretto:8-alpine-jre as production

# Setup app port
ENV API_HTTP_SERVER_PORT 5000
EXPOSE $API_HTTP_SERVER_PORT

# Setup environment
WORKDIR /app
RUN mkdir libs
RUN chown nobody:nobody -R /app
USER nobody:nobody

# Copy artifacts
COPY --from=build --chown=nobody:nobody /build/target/*.jar .
COPY --from=build --chown=nobody:nobody /build/target/libs/*.jar ./libs/
COPY --chown=nobody:nobody files ./files

CMD ["java","-jar","abordo.jar"]
