FROM alpine:3.10.3
RUN apk add --update --no-cache gettext jq

COPY ./templates /caddyfiletemplates
COPY ./createConfig.sh /createConfig.sh
RUN chmod 755 /createConfig.sh

VOLUME /configs

WORKDIR /
ENTRYPOINT ./createConfig.sh
