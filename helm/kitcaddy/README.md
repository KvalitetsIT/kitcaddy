# KitCaddy Helm Chart
Caddy 2 packages with security modules  
This chart deploys a web-service together with a KitCaddy on a Kubernetes cluster.

## Installing
First add KvalitetsIT Helm repo to Helm
```console
$ helm repo add KvalitetsIT https://raw.githubusercontent.com/KvalitetsIT/helm-repo/master/
$ helm repo update
```

Create values.yaml file with the parameters specified  
See Configuration
  
Run Helm command:  
```console
$ helm install web-service KvalitetsIT/kitcaddy -f myValues.yaml --version 1.0.3
```

Example of a value file see: [kitcaddyExampleValues.yaml](https://github.com/KvalitetsIT/kitcaddy/blob/master/helm/kitcaddyExampleValues.yaml)

## Configuration
The following table lists the configurable parameters of the KitCaddy and the web-service.

Parameter | Description | Example
--- | --- | ---
`fullnameOverride` | Name of the service
`namespace` | Namespace to deploy into
`image.repository` | Name of the web-service image 
`imgae.tag` | Web-service image tag 
`podAnnotations` | Annotations for the pod fx prometheus | `prometheus.io/path: actuator/prometheus` <br> `prometheus.io/scrape: "true"` <br> 
**KitCaddy** |
`kitcaddy.secretName` |  
`kitcaddy.extraVolumeMounts` | Extra volume mounts 
`kitcaddy.extraVolumes` | Extra volumes 
`kitcaddy.logLevel` | Set default log level. Default values 'INFO' | `DEBUG`
`kitcaddy.admin.disabled` | Set admin disabled. Default values 'true' | `true`
`kitcaddy.apps.tls.certificates.loadfiles` | Array of certificates to load from files
`kitcaddy.apps.tls.certificates.loadfiles.certificate` | Path to certificate file | `/certificates/server.cert`
`kitcaddy.apps.tls.certificates.loadfiles.key` | Path to certificate key file | `/certificates/server.key`
`kitcaddy.apps.tls.certificates.loadfiles.format` | Format of the certificate | `pem`
`kitcaddy.apps.tls.certificates.loadfiles.tags` | Array of tags
`kitcaddy.servers` | Dictionary with servers
`kitcaddy.servers.name` | Name of the server
`kitcaddy.servers.listenPort` | Port for incoming traffic | `80`
`kitcaddy.servers.strictSniHost.value` | Value for Strict SNI host. true or false | `false`
`kitcaddy.servers.tlsConnectionPolicies.clientAuthentication.require` | Value for client authentication. true or false | `true`
`kitcaddy.servers.automaticHttps.disableRedirects` | Value for disable HTTPS redirects. true or false | `true`
`kitcaddy.servers.routes` | Dictionary with routes for the server 
`kitcaddy.servers.routes.paths` | Array of path for KitCaddy to listen on
`kitcaddy.servers.routes.prometheus.path` | Prometheus path on KitCaddy. If not set Prometheus is disabled| `/metrics`
**KitCaddy - WSP** |
`kitcaddy.servers.routes.wsp` | Set values under this to enable WSP
`kitcaddy.servers.routes.wsp.mongoHost` | URL of the MongoDB | `mongodb.mongo`
`kitcaddy.servers.routes.wsp.mongoDb` | Database in Mongo to be used by the WSP | `web-service-wsp`
`kitcaddy.servers.routes.wsp.audienceRestriction` | | `urn:web-service:domain:dk`
`kitcaddy.servers.routes.wsp.sessiondataHeadername` | Header name for session data. If not set variable not in config | `sessiondataheader`
`kitcaddy.servers.routes.wsp.hok` | Hold on key | `true`
`kitcaddy.servers.routes.wsp.trusts` | Array of path to trusted certificates | `- /trust/sts.cer`
`kitcaddy.servers.routes.wsp.sslClientCertHeaderNames` | Array of SSL client cert header names. If not set variable not in config | `- surviving-bogus-header`<br>`- forwarded-from-nginx`
**KitCaddy - WSC** |
`kitcaddy.servers.routes.wsc` | Set values under this to enable WSC
`kitcaddy.servers.routes.wsc.mongoHost` | URL of the MongoDB | `mongodb.mongo`
`kitcaddy.servers.routes.wsc.mongoDb` | Database in Mongo to be used by th WSP | `web-service-wsc`
`kitcaddy.servers.routes.wsc.stsUrl` | URL to the STS server
`kitcaddy.servers.routes.wsc.clientCertFile` | Path to the client certificate file
`kitcaddy.servers.routes.wsc.clientKeyFile` | Path to the client certificate key file
`kitcaddy.servers.routes.wsc.trustCertFiles` | Array of path to trusted certificates | `- /trust/sts.cer`
`kitcaddy.servers.routes.wsc.serviceEndpoint` | Path to the service endpoint
`kitcaddy.servers.routes.wsc.serviceAudience` | 
`kitcaddy.servers.routes.wsc.sessionDataUrl` | 
**KitCaddy - SAML** |
`kitcaddy.servers.routes.saml` | Set values under this to enable SAML
`kitcaddy.servers.routes.saml.mongoHost` | URL of the MongoDB | `mongodb.mongo`
`kitcaddy.servers.routes.saml.mongoDb` | Database in Mongo to be used by the SAML | `web-service-saml`
`kitcaddy.servers.routes.saml.sessionHeaderName` | Header name for session data | `sessiondataheader` 
`kitcaddy.servers.routes.saml.sessiondataHeadername` | Header name for session data | `sessiondataheader`
`kitcaddy.servers.routes.saml.sessionExpiryHours` | Expiry time for the session in hours | `1` 
`kitcaddy.servers.routes.saml.audienceRestriction` | | `urn:web-service:domain:dk`
`kitcaddy.servers.routes.saml.idpMetadataUrl` | IDP metadata URL | `https://login.domain.dk/auth`
`kitcaddy.servers.routes.saml.entityId` | Entity ID | `urn:m:domain:dk`
`kitcaddy.servers.routes.saml.signAuthnReq` | Is sign required | `true`
`kitcaddy.servers.routes.saml.signCertFile` | Path to certificate file for sign | `/certificate/tls.crt`
`kitcaddy.servers.routes.saml.signKeyFile` | Path to certificate key file for sign | `/certificate/tls.key`
`kitcaddy.servers.routes.saml.externalUrl` |  | `https://domain.dk`
`kitcaddy.servers.routes.saml.metadataPath` | Path to metadata | `/host/saml/metadata`
`kitcaddy.servers.routes.saml.logoutPath` | Log out path | `/host/saml/logout`
`kitcaddy.servers.routes.saml.sloPath` | | `/host/saml/slo`
`kitcaddy.servers.routes.saml.ssoPath` | | `/host/saml/sso`
`kitcaddy.servers.routes.saml.logoutLandingPage` | Landing page after logout | `https://domain.dk/`
`kitcaddy.servers.routes.saml.cookieDomain` | Cookie domain | `domain.dk`
`kitcaddy.servers.routes.saml.cookiePath` | Cookie path | `/`
**KitCaddy - Upstream** |
`kitcaddy.servers.routes.upstream` | Set values under this to config upstream
`kitcaddy.servers.routes.upstream.host` | Upstream host | `localhost`
`kitcaddy.servers.routes.upstream.port` | Upstream port | `8080`
`kitcaddy.servers.routes.upstream.clientTls` | Set values under this to enable client TLS
`kitcaddy.servers.routes.upstream.clientTls.insecureSkipVerify` | Skip verity client TLS if insecure | `false`
`kitcaddy.servers.routes.upstream.clientTls.clientCertificateFile` | Path to client certificate file | `/certificate/client.crt`
`kitcaddy.servers.routes.upstream.clientTls.clientCertificateKeyFile` | Path to client certificate key file | `/certificate/client.key`
**Deployment** | 
`deployment.containerPort` | Port on web-service | `8080` 
`deplyment.readinessProbe` | Set values under this to config readiness probe
`deplyment.livenessProbe` | Set values under this to config liveness probe
**Deployment - Environment variables** |
`deployment.env` | Map of environment variables
`deployment.env.{name}` | Name of the environment variables
`deployment.env.{name}.value` | Value of the environment variable
`deployment.env.{name}.type` | Type of the environment variables. 'fieldPath' or 'secretKeyRef'. If not set classic environment variable. | `secretKeyRef`
SecretKeyRef |  
`deployment.env.{name}.name` | Name of the SecretKeyRef
`deployment.env.{name}.key` | Key for the SecretKeyRef
**Ingress** |
`ingress.enabled` | Set to true to enable ingress | `true`
`ingress.annotations` | Annotations for ingress | `kubernetes.io/ingress.class: nginx`
`ingress.hosts` | Hosts served by the ingress | `- host: domain.dk`
`ingress.tls` | TLS config 
**Service** |
`service.port` | Port on the service | `8080`
`service.targetPort` | Target port | `proxy-port`
`service.annotations` | Annotations for service | `prometheus.io/path: /manage/actuator/appmetrics`
**Documentation** | Default documentation image is `image.repository`-documentation
**Documentation Deployment** |
`docDeployment.enabled` | Enables the deployment for the documentation | `true`
`docDeployment.containerPort` | Port on documentation web-service | `8080` 
`docDeployment.readinessProbe` | Set values under this to config readiness probe
`docDeployment.livenessProbe` | Set values under this to config liveness probe
**Documentation Ingress** |
`docIngress.enabled` | Enables the ingress for the documentation | `true`
`docIngress.annotations` | Annotations for documentation ingress | `kubernetes.io/ingress.class: nginx`
`docIngress.hosts` | Hosts served by the documentation ingress | `- host: domain.dk`
`docIngress.tls` | TLS config 
**Documentation Service** |
`docService.enabled` | Enables the service for the documentation | `true`
`docService.port` | Port on the service | `8080`
`docService.targetPort` | Target port | `proxy-port`
`docService.annotations` | Annotations for service | `prometheus.io/path: /manage/actuator/appmetrics`