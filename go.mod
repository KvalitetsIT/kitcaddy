module github.com/KvalitetsIT/kitcaddy

go 1.16

require (
	github.com/KvalitetsIT/gooioidwsrest v1.1.15
	github.com/KvalitetsIT/gosamlserviceprovider v1.0.3
	github.com/KvalitetsIT/gosecurityprotocol v1.0.1
	github.com/caddyserver/caddy/v2 v2.4.5
	github.com/prometheus/client_golang v1.11.0
	github.com/prometheus/client_model v0.2.0
	go.uber.org/zap v1.19.1
	gotest.tools v2.2.0+incompatible
)

replace github.com/russellhaering/goxmldsig => github.com/evtr/goxmldsig v0.0.0-20190907195011-53d9398322c5

replace github.com/russellhaering/gosaml2 => github.com/KvalitetsIT/gosaml2 v0.0.0-20201030140015-1552cb4e4bec
