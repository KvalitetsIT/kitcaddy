package oioidwsrest

import (
	"crypto/tls"

	"fmt"
	securityprotocol "github.com/KvalitetsIT/gosecurityprotocol"
	gooioidwsrest "github.com/KvalitetsIT/gooioidwsrest"
	"strconv"
	//	"fmt"
	//	"io"
	"net/http"

	"github.com/caddyserver/caddy/v2"
	"github.com/caddyserver/caddy/v2/caddyconfig/caddyfile"
	"github.com/caddyserver/caddy/v2/caddyconfig/httpcaddyfile"
	"github.com/caddyserver/caddy/v2/modules/caddyhttp"

	"go.uber.org/zap"
	"time"
)

const DEFAULT_VALUE_SESSION_HEADER_NAME = "SESSION"

type CaddyOioIdwsRestWsc struct {
	MongoHost string `json:"mongo_host,omitempty"`

	MongoPort string `json:"mongo_port,omitempty"`

	MongoDb string `json:"mongo_db,omitempty"`

	SessionHeaderName string `json:"session_header_name,omitempty"`

	StsKombit string `json:"sts_kombit,omitempty"`

	StsUrl string `json:"sts_url,omitempty"`

	ClientCertFile string `json:"client_cert_file,omitempty"`

	ClientKeyFile string `json:"client_key_file,omitempty"`

	TrustCertFiles []string `json:"trust_cert_files,omitempty"`

	ServiceEndpoint string `json:"service_endpoint,omitempty"`

	ServiceTokenEndpoint string `json:"service_token_endpoint,omitempty"`

	ServiceAudience string `json:"service_audience,omitempty"`

	SessionDataUrl string `json:"session_data_url,omitempty"`

	ClientProtocol *gooioidwsrest.OioIdwsRestHttpProtocolClient

	Logger *zap.SugaredLogger
}

// ServeHTTP implements caddyhttp.MiddlewareHandler.
func (m CaddyOioIdwsRestWsc) ServeHTTP(w http.ResponseWriter, r *http.Request, next caddyhttp.Handler) error {

	nextService := new(CaddyService)
	nextService.Handler = next

	var maxIter = 2
	var iter = 0
	for (true) {
		wscResponseRecorder := caddyhttp.NewResponseRecorder(w, nil, nil)
		if (iter < maxIter) {
			_, err, callback := m.ClientProtocol.HandleServiceWithCallback(wscResponseRecorder, r, nextService)
			m.Logger.Debug(fmt.Sprintf("httpCode %d", wscResponseRecorder.Status()))
			if (wscResponseRecorder.Status() == http.StatusUnauthorized && callback != nil) {
				m.Logger.Debug("Status unautorized - using callback")
				(*callback)()
			} else if (wscResponseRecorder.Status() == http.StatusUnauthorized) {
				m.Logger.Debug("Status unautorized - no callback")
				iter = maxIter;
			} else {
				return err
			}
			iter += 1
		} else {
			_, err := m.ClientProtocol.HandleService(w, r, nextService)
			return err
		}
	}

	return nil
}

func init() {
	caddy.RegisterModule(CaddyOioIdwsRestWsc{})
	httpcaddyfile.RegisterHandlerDirective("oioidwsrestwsc", parseCaddyfileWsc)
}

// CaddyModule returns the Caddy module information.
func (CaddyOioIdwsRestWsc) CaddyModule() caddy.ModuleInfo {
	return caddy.ModuleInfo{
		ID: "http.handlers.oioidwsrestwsc",
		New:  func() caddy.Module { return new(CaddyOioIdwsRestWsc) },
	}
}

// Provision implements caddy.Provisioner.
func (m *CaddyOioIdwsRestWsc) Provision(ctx caddy.Context) error {
	m.Logger = ctx.Logger(m).Sugar()
	m.Logger.Info("Provisioning OioIdwsRestWsc Caddy module")
	// Create Mongo Token Cache
	mongo_port := "27017"
	if len(m.MongoPort) != 0 {
		_, conv_err := strconv.Atoi(m.MongoPort)
		if conv_err != nil {
			return conv_err
		}
		mongo_port = m.MongoPort
	}
	mongo_url := fmt.Sprintf("%s:%s", m.MongoHost, mongo_port)
	m.Logger.Debugf("Using MongoDB:%s", mongo_url)
	tokenCache, err := securityprotocol.NewMongoTokenCache(mongo_url, m.MongoDb, "wscsessions")
	if err != nil {
		m.Logger.Errorf("Can't setup tokenCache: %s", err.Error())
		return err
	}
        // maintain the tokencache regularly
        go func() {
                securityprotocol.StartMaintenance(tokenCache, 10 * time.Minute, m.Logger)
        }()


	// Maps to wsc config
	wscConfig := new(gooioidwsrest.OioIdwsRestHttpProtocolClientConfig)
	wscConfig.SessionHeaderName = DEFAULT_VALUE_SESSION_HEADER_NAME
        if (len(m.SessionHeaderName) != 0){
		wscConfig.SessionHeaderName = m.SessionHeaderName
        }

	wscConfig.SessionDataFetcher = new(securityprotocol.NilSessionDataFetcher)
	wscConfig.StsUrl = m.StsUrl
	if len(m.StsKombit) > 0 {
		useKombit, err := strconv.ParseBool(m.StsKombit)
		if err != nil {
			m.Logger.Errorf("Can't parse sts_kombit as bool: %s", err.Error())
			return err
		}
		wscConfig.UseKombitVersion = useKombit
	}
	wscConfig.TrustCertFiles = m.TrustCertFiles
	wscConfig.ClientCertFile = m.ClientCertFile
	wscConfig.ClientKeyFile = m.ClientKeyFile
	wscConfig.ServiceAudience = m.ServiceAudience
	wscConfig.ServiceEndpoint = m.ServiceEndpoint
	wscConfig.ServiceTokenEndpoint = m.ServiceTokenEndpoint

	// Create sessiondatafetcher if configured
	if len(m.SessionDataUrl) > 0 {
		m.Logger.Debugf("Setting up sessing data fetcher using URL: %s", m.SessionDataUrl)
		caCertPool := gooioidwsrest.CreateCaCertPool(m.TrustCertFiles)
		tlsConfig := &tls.Config{
			RootCAs: caCertPool,
		}
		transport := &http.Transport{TLSClientConfig: tlsConfig}
		client := &http.Client{Transport: transport}

		wscConfig.SessionDataFetcher = securityprotocol.NewServiceCallSessionDataFetcher(m.SessionDataUrl, client)
	}

	m.ClientProtocol = gooioidwsrest.NewOioIdwsRestHttpProtocolClient(*wscConfig, tokenCache, m.Logger)

	return nil
}

// Validate implements caddy.Validator.
func (m *CaddyOioIdwsRestWsc) Validate() error {

	if len(m.MongoHost) == 0 {
		return fmt.Errorf("mongo_host must be configured")
	}

	if len(m.MongoDb) == 0 {
		return fmt.Errorf("mongo_db must be configured")
	}

	if len(m.StsUrl) == 0 {
		return fmt.Errorf("sts_url must be configured")
	}

	if len(m.ServiceEndpoint) == 0 {
		return fmt.Errorf("service_endpoint must be configured")
	}

	return nil
}

// UnmarshalCaddyfile implements caddyfile.Unmarshaler.
func (m *CaddyOioIdwsRestWsc) UnmarshalCaddyfile(d *caddyfile.Dispenser) error {
	for d.Next() {
		//if !d.Args(&m.Output) {
		//	return d.ArgErr()
		//}
	}
	return nil
}

// parseCaddyfile unmarshals tokens from h into a new Middleware.
func parseCaddyfileWsc(h httpcaddyfile.Helper) (caddyhttp.MiddlewareHandler, error) {
	var m CaddyOioIdwsRestWsc
	err := m.UnmarshalCaddyfile(h.Dispenser)
	return m, err
}

// Interface guards
var (
	_ caddy.Provisioner           = (*CaddyOioIdwsRestWsc)(nil)
	_ caddy.Validator             = (*CaddyOioIdwsRestWsc)(nil)
	_ caddyhttp.MiddlewareHandler = (*CaddyOioIdwsRestWsc)(nil)
	_ caddyfile.Unmarshaler       = (*CaddyOioIdwsRestWsc)(nil)
)
