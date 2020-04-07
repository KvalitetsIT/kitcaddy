package main

import (
	caddycmd "github.com/caddyserver/caddy/v2/cmd"

	// plug in Caddy modules here
	_ "github.com/caddyserver/caddy/v2/modules/standard"
	_ "kitcaddy/modules/saml"
	_ "kitcaddy/modules/prometheus"
	_ "kitcaddy/modules/oioidwsrest"
)

func main() {
	caddycmd.Main()
}
