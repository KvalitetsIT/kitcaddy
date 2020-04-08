package dk.kvalitetsit.kitcaddy.test;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 *    This testsetup
 * 
 *    | Webbrowser |    ->    | SAML-SP |   ->   | wsc | ->   | wsp | -> | echoservice | 
 *
 */

public class AllInOneIntegrationTest extends AbstractBrowserBasedIntegrationTest {

	public static final String 	SAML_SP_HOST 	= "uiservice";
	public static final int 	SAML_SP_PORT 	= 8787;
	public static final String 	SAML_SP_URL 	= SAML_SP_HOST+":"+SAML_SP_PORT;
	
	private static final String WSC_SERVICE_HOST = "wsc";
	private static final int WSC_SERVICE_PORT = 8686;
	private static final String WSC_SERVICE_URL = WSC_SERVICE_HOST+":"+WSC_SERVICE_PORT;

	private static final String WSP_SERVICE_HOST = "testserviceaa";
	private static final int WSP_SERVICE_PORT = 8443;
	private static final String WSP_SERVICE_URL = WSP_SERVICE_HOST+":"+WSP_SERVICE_PORT;

	@Rule
	public BrowserWebDriverContainer<?> chrome = createChrome();

	@Rule
	public GenericContainer<?> samlSp = createSamlSp();

	@Rule
	public GenericContainer<?> wsc = createWsc();

	@Rule
	public GenericContainer<?> wsp = createWsp();

	public static GenericContainer<?> createWsc() {
		return 	getKitCaddyContainer(WSC_SERVICE_HOST, WSC_SERVICE_PORT, getDockerNetwork(), "wsc/wsc.config");
	}

	public static GenericContainer<?> createWsp() {
		return 	getKitCaddyContainer(WSP_SERVICE_HOST, WSP_SERVICE_PORT, getDockerNetwork(), "wsp/wsp.config");
	}

	public static GenericContainer<?> createSamlSp() {
		return 	getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
	}
	
	@Test
	public void testAccessProtectedRessouceCorrectUsernamePassword() throws JSONException, JsonMappingException, JsonProcessingException {
		
		// Given
		String username = "test123";
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();
		
		// When
		String result = doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/service/test", username, password);
		
		// Then
		Assert.assertTrue("Expected to find the start of JSON data", result.indexOf("{") >= 0 );
		Assert.assertTrue("Expected to find the end of JSON data", result.lastIndexOf("}") >= 0 );
		String jsonReturned = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1);
		JsonNode responseParsed = new ObjectMapper().readValue(jsonReturned, JsonNode.class);
		Assert.assertNotNull(responseParsed);
	}

	public String getSpServiceUrl() {
		return "http://"+samlSp.getContainerIpAddress()+":"+samlSp.getMappedPort(SAML_SP_PORT);
	}
}
