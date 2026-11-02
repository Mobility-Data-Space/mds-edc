FROM eclipse-temurin:25.0.5_8-jre-alpine-3.24

RUN apk -U upgrade --no-cache

ARG RUNTIME
ENV RUNTIME=${RUNTIME}

WORKDIR /app

COPY ${RUNTIME}/build/distributions/${RUNTIME}-*.tar /app/

RUN tar --strip-components=1 -xf ${RUNTIME}-*.tar && rm ${RUNTIME}-*.tar

ENTRYPOINT /app/bin/${RUNTIME}
