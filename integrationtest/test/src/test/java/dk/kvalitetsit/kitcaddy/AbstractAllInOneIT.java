package dk.kvalitetsit.kitcaddy;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.testcontainers.selenium.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;

import dk.kvalitetsit.kitcaddy.test.configuration.AllInOneTestConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 
 *    This testsetup
 * 
 *    | Webbrowser |    ->    | SAML-SP |   ->   | wsc | ->   | wsp | -> | echoservice | 
 *
 */
@Testcontainers
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=AllInOneTestConfiguration.class, loader=AnnotationConfigContextLoader.class)
public abstract class AbstractAllInOneIT extends AbstractBrowserBasedIntegrationTest {

	public static final String 	SAML_SP_HOST 	= "uiservice";
	public static final int 	SAML_SP_PORT 	= 8787;
	public static final String 	SAML_SP_URL 	= SAML_SP_HOST+":"+SAML_SP_PORT;
	
	private static final String WSC_SERVICE_HOST = "wsc";
	private static final int WSC_SERVICE_PORT = 8686;
	private static final String WSC_SERVICE_URL = WSC_SERVICE_HOST+":"+WSC_SERVICE_PORT;

	private static final String WSP_SERVICE_HOST = "testserviceaa";
	private static final int WSP_SERVICE_PORT = 8443;
	private static final String WSP_SERVICE_URL = WSP_SERVICE_HOST+":"+WSP_SERVICE_PORT;

	@Container
	public BrowserWebDriverContainer chrome = createChrome();

	@Container
	public GenericContainer<?> samlSp = createSamlSp();

	@Container
	public GenericContainer<?> wsc = createWsc();

	@Container
	public GenericContainer<?> wsp = createWsp();

	public static GenericContainer<?> createWsc() {
		return 	getKitCaddyContainer(WSC_SERVICE_HOST, WSC_SERVICE_PORT, getDockerNetwork(), "wsc/wsc.config");
	}

	public static GenericContainer<?> createWsp() {
		return 	getKitCaddyContainer(WSP_SERVICE_HOST, WSP_SERVICE_PORT, getDockerNetwork(), "wsp/wsp.config");
	}

	public static GenericContainer<?> createSamlSp() {
		return getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
	}

	public String getSpServiceUrl() {
		return "http://"+ samlSp.getHost() +":"+samlSp.getMappedPort(SAML_SP_PORT);
	}
}
