package main

import (
	caddycmd "github.com/caddyserver/caddy/v2/cmd"

	// plug in Caddy modules here
	_ "github.com/caddyserver/caddy/v2/modules/standard"
	_ "github.com/KvalitetsIT/kitcaddy/modules/saml"
	_ "github.com/KvalitetsIT/kitcaddy/modules/prometheus"
	_ "github.com/KvalitetsIT/kitcaddy/modules/oioidwsrest"
)

func main() {
	caddycmd.Main()
}
