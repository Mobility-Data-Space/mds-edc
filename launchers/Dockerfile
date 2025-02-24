FROM eclipse-temurin:23.0.2_7-jre-alpine

ARG RUNTIME
ENV RUNTIME=${RUNTIME}

WORKDIR /app

COPY ${RUNTIME}/build/distributions/connector-*.tar /app/

RUN tar --strip-components=1 -xf connector-*.tar && rm connector-*.tar

ENTRYPOINT /app/bin/${RUNTIME}
