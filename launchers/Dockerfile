FROM eclipse-temurin:21.0.6_7-jre-alpine-3.21

ARG RUNTIME
ENV RUNTIME=${RUNTIME}

WORKDIR /app

COPY ${RUNTIME}/build/distributions/connector-*.tar /app/

RUN tar --strip-components=1 -xf connector-*.tar && rm connector-*.tar

ENTRYPOINT /app/bin/${RUNTIME}
