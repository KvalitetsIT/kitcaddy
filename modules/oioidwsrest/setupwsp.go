package oioidwsrest

import (

	"fmt"
	"strconv"
	gooioidwsrest "github.com/KvalitetsIT/gooioidwsrest"
	securityprotocol "github.com/KvalitetsIT/gosecurityprotocol"
	"net/http"
	"github.com/caddyserver/caddy/v2"
	"github.com/caddyserver/caddy/v2/caddyconfig/caddyfile"
	"github.com/caddyserver/caddy/v2/caddyconfig/httpcaddyfile"
	"github.com/caddyserver/caddy/v2/modules/caddyhttp"

	"time"
	"go.uber.org/zap"
)


type CaddyOioIdwsRestWsp struct {

	MongoHost string `json:"mongo_host,omitempty"`

	MongoPort string `json:"mongo_port,omitempty"`

	MongoDb string `json:"mongo_db,omitempty"`

	TrustCertFiles []string `json:"trust_cert_files,omitempty"`

	AudienceRestriction string `json:"audience_restriction,omitempty"`

	HoK string `json:"hok,omitempty"`

	SessiondataHeaderName string `json:"sessiondata_headername,omitempty"`

	ProviderProtocol *gooioidwsrest.OioIdwsRestWsp

	Logger *zap.SugaredLogger
}

// ServeHTTP implements caddyhttp.MiddlewareHandler.
func (m CaddyOioIdwsRestWsp) ServeHTTP(w http.ResponseWriter, r *http.Request, next caddyhttp.Handler) error {

	nextService := new(CaddyService)
	nextService.Handler = next

	httpCode, err := m.ProviderProtocol.HandleService(w, r, nextService)
	if (httpCode != http.StatusOK) {
		return caddyhttp.Error(httpCode, err)
	}

	return nil
}



func init() {
	caddy.RegisterModule(CaddyOioIdwsRestWsp{})
	httpcaddyfile.RegisterHandlerDirective("oioidwsrestwsp", parseCaddyfileWsc)
}

// CaddyModule returns the Caddy module information.
func (CaddyOioIdwsRestWsp) CaddyModule() caddy.ModuleInfo {
	return caddy.ModuleInfo{
		ID: "http.handlers.oioidwsrestwsp",
		New:  func() caddy.Module { return new(CaddyOioIdwsRestWsp) },
	}
}

// Provision implements caddy.Provisioner.
func (m *CaddyOioIdwsRestWsp) Provision(ctx caddy.Context) error {
    m.Logger = ctx.Logger(m).Sugar()
    m.Logger.Info("Provisioning OioIdwsRestWsp Caddy module")
	// Create Mongo Token Cache
	mongo_port := "27017"
	if (len(m.MongoPort) != 0) {
		_, conv_err := strconv.Atoi(m.MongoPort)
        	if (conv_err != nil) {
                	return conv_err
        	}
		mongo_port = m.MongoPort
        }
	mongo_url := fmt.Sprintf("%s:%s", m.MongoHost, mongo_port)
	m.Logger.Debugf("Using MongoDB: %s", mongo_url)
	sessionCache, err := securityprotocol.NewMongoSessionCache(mongo_url, m.MongoDb, "sessions")
	if (err != nil) {
	    m.Logger.Errorf("Can't setup tokenCache: %s", err.Error())
		return err
	}
        // maintain the sessioncache regularly
        go func() {
                securityprotocol.StartMaintenance(sessionCache, 10 * time.Minute, m.Logger)
        }()


	// Maps to wsp config
        wspConfig := new(gooioidwsrest.OioIdwsRestHttpProtocolServerConfig)
        wspConfig.TrustCertFiles = m.TrustCertFiles
        wspConfig.AudienceRestriction = m.AudienceRestriction

	if (len(m.HoK) > 0) {
		wspConfig.HoK, _ = strconv.ParseBool(m.HoK)
	} else {
		wspConfig.HoK = true
	}

	if (len(m.SessiondataHeaderName) > 0) {
		wspConfig.SessiondataHeaderName = m.SessiondataHeaderName
	}

	m.ProviderProtocol = gooioidwsrest.NewOioIdwsRestWspFromConfig(wspConfig, sessionCache, m.Logger)

	return nil
}

// Validate implements caddy.Validator.
func (m *CaddyOioIdwsRestWsp) Validate() error {

	if (len(m.MongoHost) == 0) {
		return fmt.Errorf("mongo_host must be configured")
	}

        if (len(m.MongoDb) == 0) {
                return fmt.Errorf("mongo_db must be configured")
        }

	if (len(m.HoK) > 0) {
		_, err := strconv.ParseBool(m.HoK)
		if (err != nil) {
			return fmt.Errorf(fmt.Sprintf("Error converting hok value: %s", err.Error()))
		}
	}

	return nil
}


// UnmarshalCaddyfile implements caddyfile.Unmarshaler.
func (m *CaddyOioIdwsRestWsp) UnmarshalCaddyfile(d *caddyfile.Dispenser) error {
	for d.Next() {
		//if !d.Args(&m.Output) {
		//	return d.ArgErr()
		//}
	}
	return nil
}

// parseCaddyfile unmarshals tokens from h into a new Middleware.
func parseCaddyfileWsp(h httpcaddyfile.Helper) (caddyhttp.MiddlewareHandler, error) {
	var m CaddyOioIdwsRestWsp
	err := m.UnmarshalCaddyfile(h.Dispenser)
	return m, err
}

// Interface guards
var (
	_ caddy.Provisioner              = (*CaddyOioIdwsRestWsp)(nil)
	_ caddy.Validator                = (*CaddyOioIdwsRestWsp)(nil)
	_ caddyhttp.MiddlewareHandler    = (*CaddyOioIdwsRestWsp)(nil)
	_ caddyfile.Unmarshaler          = (*CaddyOioIdwsRestWsp)(nil)
)
