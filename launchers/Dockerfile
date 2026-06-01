FROM eclipse-temurin:26-jre-alpine-3.23

ARG RUNTIME
ENV RUNTIME=${RUNTIME}

WORKDIR /app

COPY ${RUNTIME}/build/distributions/connector-*.tar /app/

RUN tar --strip-components=1 -xf connector-*.tar && rm connector-*.tar

ENTRYPOINT /app/bin/${RUNTIME}
