module kitcaddy

go 1.14

replace github.com/KvalitetsIT/gooioidwsrest => github.com/KvalitetsIT/gooioidwsrest v0.0.0-20200907194144-625faa8a273f

replace github.com/KvalitetsIT/gosecurityprotocol => github.com/KvalitetsIT/gosecurityprotocol v0.0.0-20200416184625-51822bff6698
                                                                                           
replace github.com/KvalitetsIT/gosamlserviceprovider => github.com/KvalitetsIT/gosamlserviceprovider/samlprovider e4d6ae23f01bc059f99b1a662422f1f0b7fe8219

require (

        github.com/KvalitetsIT/gooioidwsrest v0.0.0-20200907194144-625faa8a273f

	github.com/KvalitetsIT/gosecurityprotocol v0.0.0-20200416184625-51822bff6698

        github.com/KvalitetsIT/gosamlserviceprovider e4d6ae23f01bc059f99b1a662422f1f0b7fe8219

	github.com/caddyserver/caddy/v2 v2.0.0-rc.3

	github.com/Masterminds/sprig/v3 v3.0.2
	github.com/alecthomas/chroma v0.7.2-0.20200305040604-4f3623dce67a
	github.com/caddyserver/certmagic v0.10.7
	github.com/dustin/go-humanize v1.0.1-0.20200219035652-afde56e7acac
	github.com/go-acme/lego/v3 v3.5.0
	github.com/google/cel-go v0.4.1
	github.com/jsternberg/zap-logfmt v1.2.0
	github.com/klauspost/compress v1.10.3
	github.com/klauspost/cpuid v1.2.3
	github.com/lucas-clemente/quic-go v0.15.2
	github.com/manifoldco/promptui v0.7.0 // indirect
	github.com/miekg/dns v1.1.29 // indirect
	github.com/naoina/go-stringutil v0.1.0 // indirect
	github.com/naoina/toml v0.1.1
	github.com/smallstep/certificates v0.14.0-rc.5
	github.com/smallstep/cli v0.14.0-rc.3
	github.com/smallstep/truststore v0.9.5
	github.com/vulcand/oxy v1.1.0
	github.com/yuin/goldmark v1.1.27
	github.com/yuin/goldmark-highlighting v0.0.0-20200307114337-60d527fdb691
	go.uber.org/zap v1.14.1
	golang.org/x/crypto v0.0.0-20200323165209-0ec3e9974c59
	golang.org/x/net v0.0.0-20200324143707-d3edc9973b7e
	google.golang.org/genproto v0.0.0-20200323114720-3f67cca34472
	gopkg.in/natefinch/lumberjack.v2 v2.0.0
	gopkg.in/square/go-jose.v2 v2.4.1 // indirect
	gopkg.in/yaml.v2 v2.2.8

	gotest.tools v2.2.0+incompatible
)

replace github.com/russellhaering/goxmldsig => github.com/evtr/goxmldsig v0.0.0-20190907195011-53d9398322c5
replace github.com/russellhaering/gosaml2 => github.com/KvalitetsIT/gosaml2 v0.0.0-20201030140015-1552cb4e4bec

