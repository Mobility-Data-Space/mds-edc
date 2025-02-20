FROM eclipse-temurin:23.0.2_7-jre-alpine

WORKDIR /app

COPY build/distributions/connector-*.tar /app/

RUN tar --strip-components=1 -xf connector-*.tar && rm connector-*.tar

ENTRYPOINT ["/app/bin/connector"]
