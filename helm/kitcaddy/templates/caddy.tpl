{{- define "kitcaddy.servers" -}}
    {{- $temp := dict "servers" (list) -}}
    {{- range $name, $server := .Values.kitcaddy.servers }}
        {{- $noop := include "kitcaddy.server" $server | nindent 12 | append $temp.servers | set $temp "servers" -}}
    {{- end}}
    {{- join "," $temp.servers -}}
{{- end}}

{{- define "kitcaddy.server" }}
{{ .name | quote}}: {
  "listen": [
    ":{{.listenPort}}"
  ],
  {{- if (.strictSniHost) }}
  "strict_sni_host": {{ .strictSniHost.value }},
  {{- end }}
  {{- if (.tlsConnectionPolicies) }}
  "tls_connection_policies": [{
  {{- if (.tlsConnectionPolicies.clientAuthentication) }}
  {{- with .tlsConnectionPolicies.clientAuthentication }}
    "client_authentication": {
       {{- if (.require) }}
       "require": {{.require}}
       {{- end }}
    }
  {{- end }}
  {{- end }}
  }],
  {{- end }}
  {{- if (.automaticHttps) }}
  "automatic_https": {
  {{- if (.automaticHttps.disableRedirects) }}
    "disable_redirects": {{.automaticHttps.disableRedirects}}
  {{- end }}
  },
  {{- end }}
  "routes": [
     {{- template "kitcaddy.routes" . }}
  ]
}

{{- end}}

{{- define "kitcaddy.routes"}}
    {{- $temp := dict "routes" (list) -}}
    {{- range $route := .routes }}
        {{- $noop := include "kitcaddy.route" $route | nindent 6 | append $temp.routes | set $temp "routes" -}}
    {{- end}}
    {{- join "," $temp.routes -}}
{{- end}}

{{- define "kitcaddy.route"}}
{
   "match": [
    {
      "path": [
      {{- range $index, $path := .paths }}
      {{- if $index}},{{- end}}
        {{$path | quote }}
      {{- end }}
      ]
    }
   ],
   "handle": [
    {{- if (.prometheus)}}
    {
      "handler": "prometheus",
      "metrics_path": {{ .prometheus.path | quote }}
    },
    {{- end }}
    {{- if (.wsp) }}
    {{- with .wsp }}
    {
      "handler": "oioidwsrestwsp",
      "mongo_host": {{ .mongoHost | quote }},
      "mongo_db": {{ .mongoDb | quote }},
      "trust_cert_files": [
        {{- range $index, $trust := .trusts }}
        {{- if $index}},{{- end}}
        {{ $trust | quote }}
        {{- end }}
      ],
      "audience_restriction": {{ .audienceRestriction | quote }},
      {{- if (.sessiondataHeadername) }}
      "sessiondata_headername": {{ .sessiondataHeadername | quote }},
      {{- end }}
      {{- if (.sslClientCertHeaderNames) }}
      "ssl_client_cert_header_names": [
      {{- range $index, $names := .sslClientCertHeaderNames }}
      {{- if $index}},{{- end}}
        {{ $names | quote }}
      {{- end }}
      ],
      {{- end }}
      "hok": {{ .hok | quote }}
    },
    {{- end }}
    {{- end }}
    {{- if (.wsc) }}
    {{- with .wsc }}
    {
      "handler": "oioidwsrestwsc",
      "mongo_host": {{ .mongoHost | quote }},
      "mongo_db": {{ .mongoDb | quote }},
      {{- if (.sessionHeaderName) }}
      "session_header_name": {{ .sessionHeaderName | quote }},
      {{- end }}
      "sts_url": {{ .stsUrl | quote }},
      "client_cert_file": {{ .clientCertFile | quote }},
      "client_key_file": {{ .clientKeyFile | quote }},
      "trust_cert_files": [
      {{- range $index, $trust := .trustCertFiles }}
        {{- if $index}},{{- end}}
        {{ $trust | quote }}
      {{- end }}
      ],
      {{- if (.sessionDataUrl) }}
        "session_data_url": {{ .sessionDataUrl | quote }},
      {{- end }}
      "service_endpoint": {{ .serviceEndpoint | quote }},
      "service_audience": {{ .serviceAudience | quote }}
    },
    {{- end }}
    {{- end }}
    {{- if (.saml) }}
    {{- with .saml }}
    {
      "handler": "samlprovider",
      "session_header_name": {{ .sessionHeaderName | quote }},
      {{- if (.sessiondataHeadername) }}
      "sessiondata_headername": {{ .sessiondataHeadername | quote }},
      {{- end }}
      "session_expiry_hours": {{ .sessionExpiryHours | quote }},
      "mongo_host": {{ .mongoHost | quote }},
      "mongo_db": {{ .mongoDb | quote }},
      "audience_restriction": {{ .audienceRestriction | quote }},
      "idp_metadata_url": {{ .idpMetadataUrl | quote }},
      "entityId": {{ .entityId | quote }},
      "sign_authn_req": {{ .signAuthnReq | quote }},
      "sign_cert_file": {{ .signCertFile | quote }},
      "sign_key_file": {{ .signKeyFile | quote }},
      "external_url": {{ .externalUrl | quote }},
      "metadata_path": {{ .metadataPath | quote }},
      "logout_path": {{ .logoutPath | quote }},
      "slo_path": {{ .sloPath | quote }},
      "sso_path": {{ .ssoPath | quote }},
      "logout_landing_page": {{ .logoutLandingPage | quote }},
      "cookie_domain": {{ .cookieDomain | quote }},
      {{- if (.roleAttributeName) }}
      "role_attribute_name": {{ .roleAttributeName | quote }},
      {{- end }}
      {{- if (.allowedRoles) }}
      "allowed_roles": {{ .allowedRoles | toJson}},
      {{- end }}
      "cookie_path": {{ .cookiePath | quote }}

    },
    {{- end }}
    {{- end }}
    {
      "handler": "reverse_proxy",
      "transport": {
        "protocol": "http",
        {{- if (.upstream.clientTls) }}
        {{- with .upstream.clientTls }}
        "tls": {
          "insecure_skip_verify": {{ .insecureSkipVerify }},
          "client_certificate_file": {{ .clientCertificateFile | quote }},
          "client_certificate_key_file": {{ .clientCertificateKeyFile | quote }}
        },
        {{- end }}
        {{- end }}
        "read_buffer_size": 4096
    },
    "upstreams": [
      {
        "dial": "{{ .upstream.host }}:{{ .upstream.port }}"
      }
    ]
    {{- if (.wsc) }}
    ,
    "headers": {
      "request": {
        "set": {
          "Host": ["{{ .upstream.host }}:{{ .upstream.port }}"]
        }
      }
    }
    {{- end }}
  }
 ]
}

{{- end }}