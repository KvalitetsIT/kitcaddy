# KitCaddy

KitCaddy is an open source Web Server written in Golang, with a Kit (KvalitetsIT) extension for hanlig OIO IDWS REST
security protocol. See more <a href="https://www.digitaliser.dk/resource/3457606">here</a> about OIO IDWS REST. KitCaddy
works as a reverse proxy in front of the webservice. That's decouple the security protocol from the webservice.

![](documentation/KitCaddy-overview.png)

The images show the three configurations possible for KitCaddy; SAML, WSC and WSP. Configuration of the KitCaddy is done
by configuration file ore with the Helm Chart.

The Helm Chart is using KitCaddy as a sidecar to the selected webservice. As shown on the image above. See Helm Chart
documentation <a href="https://github.com/KvalitetsIT/kitcaddy/tree/master/helm/kitcaddy">here</a>

If configured by configuration file, KitCaddy expecting a configuration file placed here /config/configmap.yaml

The flow whit WSC and WSP this shown in the flowchart below.
![](documentation/WSC-WSP-flow.png)

## How to test a STS with KitCaddy as WSC and WSP

In `/integrationtest/compose-setups/wsc-sts-wsp` a docker `compose.yaml`  file can be found. This will start two
KitCaddy images, one as a WSC and one as a WSP, a database used by the WSC and WSP, and an HTTP echo service.
Under `wsc-sts-wsp` a `config` folder exists, which contain files to configure the WSC and WSP. These config files are
Caddy JSON configuration structures. For details of the configuration files, look for the JSON configuration structure
documentation on the <a href="https://caddyserver.com/">Caddy website</a>.

By default, the WSP is configured to forward traffic to the echo service. This can be configured in the WSP config -
look for `"dial": "echo:80"`, and change `echo:80`
accordingly. Replace `TODO` in the WSC config with the URL to the STS. Replace the `stsTODO.cer` in the `compose.yaml`
with the public certificate from the STS, so that the WSC and the WSP can add it to their trusted certificates.

Run the setup with `docker-compose up`, and visit <a href="localhost:8080">localhost:8080</a> in your browser to verify
that the STS correctly pass a token to the WCS and allow access to the service that the WSP is connected to.

## How to run the tests

In order to run the `KitCaddy` tests, first build the `KitCaddy` docker image locally:

```
    docker build -t kvalitetsit/kitcaddy:dev .
```

Then go to `/integrationtest` and run `mvn test`.