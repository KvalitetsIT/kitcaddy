#FROM golang:1.16.9 AS builder
FROM golang:1.25.8-alpine@sha256:8e02eb337d9e0ea459e041f1ee5eece41cbb61f1d83e7d883a3e2fb4862063fa AS builder

#non-root
RUN addgroup --gid 11000 appuser && \
    adduser -G appuser --uid 11000 appuser -D

ENV GO111MODULE=on

# Prepare for custom caddy build
RUN mkdir /kitcaddy
WORKDIR /kitcaddy
COPY go.mod .
COPY go.sum .
RUN go mod download

COPY modules /kitcaddy/modules
COPY caddy /kitcaddy/caddy
COPY main.go main.go

# Install certs
RUN apk --update add ca-certificates

#RUN go test -coverprofile=coverage.out -v ./...
#RUN go tool cover -func=coverage.out
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -ldflags="-w -s" -o /go/bin/kitcaddy .
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -ldflags="-w -s" -o /go/bin/caddy ./caddy

FROM scratch

COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=builder /go/bin/caddy /usr/bin/caddy
COPY --from=builder /etc/passwd /etc/passwd
COPY --from=builder /etc/group /etc/group

USER 11000
ENTRYPOINT ["/usr/bin/caddy", "run"]