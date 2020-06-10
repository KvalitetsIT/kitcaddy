# KitCaddy
Caddy 2 packaged with security modules

## Introduction
This chart deploys a web-service with a KitCaddy on a Kubernetes cluster.

## Installing
First add KvalitetsIT Helm repo to Helm
```console
$ helm repo add KvalitetsIT https://kvalitetsit.github.io/helm-chart
$ helm repo update
```

Create values.yaml file with the parameters specified  
See Configuration
  
Run Helm command:  
```console
$ helm install web-service KvalitetsIT/kitcaddy -f myValues.yaml --version 1.0.3
```


## Configuration
The following table lists the configurable parameters of the KitCaddy and the web-service.

Parameter | Description | Example
--- | --- | ---
`image.repository` | Name of the web-service image 
`imgae.tag` | Web-service image tag 
**KitCaddy** |
`kitcaddy.secretName` |  
`kitcaddy.extraVolumeMounts` | Array of extra volume mounts 
`kitcaddy.extraVolumes` | Array of extra volumes 
`kitcaddy.servers` | Array of servers
`kitcaddy.servers.name` | Name of the server
`kitcaddy.servers.listenPort` | Port for incoming traffic | `80`
`kitcaddy.servers.paths` | Array of paths for KitCaddy to listen on
`kitcaddy.servers.prometheus.enable` | Set true or false for Prometheus on KitCaddy | `true`
`kitcaddy.servers.prometheus.path` | Prometheus path on KitCaddy | `/metrics`
**KitCaddy - WSP** |
`kitcaddy.servers.wsp` | Set values under this to enable WSP
`kitcaddy.servers.wsp.mongoHost` | URL of the MongoDB | `mongodb.mongo`
`kitcaddy.servers.wsp.mongoDb` | Database in Mongo to be used by the WSP | `web-service-wsp`
`kitcaddy.servers.wsp.audienceRestriction` | | `urn:web-service:domain:dk`
`kitcaddy.servers.wsp.sessiondataHeadername` | Header name for session data | `sessiondataheader`
`kitcaddy.servers.wsp.hok` | Hold on key | `true`
`kitcaddy.servers.wsp.trusts` | Array of path to trusted certificates | `- /trust/sts.cer`
**KitCaddy - WSC** |
`kitcaddy.servers.wsc` | Set values under this to enable WSC
`kitcaddy.servers.wsc.mongoHost` | URL of the MongoDB | `mongodb.mongo`
`kitcaddy.servers.wsc.mongoDb` | Database in Mongo to be used by th WSP | `web-service-wsc`
`kitcaddy.servers.wsc.stsUrl` | URL to the STS server
`kitcaddy.servers.wsc.clientCertFile` | Path to the client certificate file
`kitcaddy.servers.wsc.clientKeyFile` | Path to the client certificate key file
`kitcaddy.servers.wsc.trustCertFiles` | Array of path to trusted certificates | `- /trust/sts.cer`
`kitcaddy.servers.wsc.serviceEndpoint` | Path to the service endpoint
`kitcaddy.servers.wsc.serviceAudience` | 
`kitcaddy.servers.wsc.sessionDataUrl` | 
**KitCaddy - SAML** |
`kitcaddy.servers.saml` | Set values under this to enable SAML
`kitcaddy.servers.saml.mongoHost` | URL of the MongoDB | `mongodb.mongo`
`kitcaddy.servers.saml.mongoDb` | Database in Mongo to be used by the SAML | `web-service-saml`
`kitcaddy.servers.saml.sessionHeaderName` | Header name for session data | `sessiondataheader` 
`kitcaddy.servers.saml.sessionExpiryHours` | Expiry time for the session in hours | `1` 
`kitcaddy.servers.saml.audienceRestriction` | | `urn:web-service:domain:dk`
`kitcaddy.servers.saml.idpMetadataUrl` | IDP metadata URL | `https://login.domain.dk/auth`
`kitcaddy.servers.saml.entityId` | Entity ID | `urn:m:domain:dk`
`kitcaddy.servers.saml.signAuthnReq` | Is sign required | `true`
`kitcaddy.servers.saml.signCertFile` | Path to certificate file for sign | `/certificate/tls.crt`
`kitcaddy.servers.saml.signKeyFile` | Path to certificate key file for sign | `/certificate/tls.key`
`kitcaddy.servers.saml.externalUrl` |  | `https://domain.dk`
`kitcaddy.servers.saml.metadataPath` | Path to metadata | `/host/saml/metadata`
`kitcaddy.servers.saml.logoutPath` | Log out path | `/host/saml/logout`
`kitcaddy.servers.saml.sloPath` | | `/host/saml/slo`
`kitcaddy.servers.saml.ssoPath` | | `/host/saml/sso`
`kitcaddy.servers.saml.logoutLandingPage` | Landing page after logout | `https://domain.dk/`
`kitcaddy.servers.saml.cookieDomain` | Cookie domain | `domain.dk`
`kitcaddy.servers.saml.cookiePath` | Cookie path | `/`
**KitCaddy - Upstream** |
`kitcaddy.servers.upstream` | Set values under this to config upstream
`kitcaddy.servers.upstream.host` | Upstream host | `localhost`
`kitcaddy.servers.upstream.port` | Upstream port | `8080`
`kitcaddy.servers.upstream.clientTls` | Set values under this to enable client TLS
`kitcaddy.servers.upstream.clientTls.insecureSkipVerify` | Skip verity client TLS if insecure | `false`
`kitcaddy.servers.upstream.clientTls.clientCertificateFile` | Path to client certificate file | `/certificate/client.crt`
`kitcaddy.servers.upstream.clientTls.clientCertificateKeyFile` | Path to client certificate key file | `/certificate/client.key`
**Deployment** | 
`deployment.containerPort` | Port on web-service | `8080` 
`deployment.env` | Array of environment variables 
**Service** |
`service.port` | Port on the service | `8080`
`service.targetPort` | Target port | `proxy-port`






