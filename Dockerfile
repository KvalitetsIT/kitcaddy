FROM golang:1.13.4 as builder
ENV GO111MODULE=on

# Prepare for custom caddy build
RUN mkdir /kitcaddy
WORKDIR /kitcaddy
ADD go.mod go.mod
RUN go mod download

COPY modules /kitcaddy/modules
COPY caddy /kitcaddy/caddy
COPY main.go main.go

RUN go test -coverprofile=coverage.out -v ./...
RUN go tool cover -func=coverage.out
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o /go/bin/kitcaddy .
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o /go/bin/caddy ./caddy

FROM alpine:3.10.3 as certs
RUN apk --update add ca-certificates

FROM scratch
#alpine:3.10.3
#scratch
COPY --from=certs /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=builder /go/bin/caddy /usr/bin/caddy
ENTRYPOINT ["/usr/bin/caddy", "run"]

