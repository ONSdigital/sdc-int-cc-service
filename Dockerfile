FROM openjdk:17

RUN groupadd -g 983 ccsvc && \
    useradd -r -u 983 -g ccsvc ccsvc
USER ccsvc
COPY target/ccsvc-0.0.0.jar /opt/ccsvc.jar

ENTRYPOINT [ "java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/opt/ccsvc.jar" ]
