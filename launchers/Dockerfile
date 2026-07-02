FROM eclipse-temurin:26-jre-alpine-3.23

# Pull Alpine security patches not yet picked up by the upstream Temurin image
# (e.g. OpenSSL 3.5.7-r0 / CVE-2026-45447). Re-evaluate when Temurin republishes.
RUN apk -U upgrade --no-cache

ARG RUNTIME
ENV RUNTIME=${RUNTIME}

WORKDIR /app

COPY ${RUNTIME}/build/distributions/connector-*.tar /app/

RUN tar --strip-components=1 -xf connector-*.tar && rm connector-*.tar

ENTRYPOINT /app/bin/${RUNTIME}
