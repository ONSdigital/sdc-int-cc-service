FROM openjdk:openjdk:17

RUN apt-get update
RUN apt-get -yq clean
RUN groupadd -g 983 ccsvc && \
    useradd -r -u 983 -g ccsvc ccsvc
USER ccsvc
COPY target/ccsvc-0.0.0.jar /opt/ccsvc.jar

ENTRYPOINT [ "java", "-jar", "/opt/ccsvc.jar" ]

