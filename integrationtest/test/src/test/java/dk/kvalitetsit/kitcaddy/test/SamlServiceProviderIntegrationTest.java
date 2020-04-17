package dk.kvalitetsit.kitcaddy.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.kvalitetsit.kitcaddy.AbstractBrowserBasedIntegrationTest;

/**
 * 
 *    This testsetup
 * 
 *    | Webbrowser |    ->    | SAML-SP |    ->    | echoservice | 
 *
 */
public class SamlServiceProviderIntegrationTest extends AbstractBrowserBasedIntegrationTest {

	public static final String 	SAML_SP_HOST 	= "uiservice";
	public static final int 	SAML_SP_PORT 	= 8787;
	public static final String 	SAML_SP_URL 	= SAML_SP_HOST+":"+SAML_SP_PORT;

	@Rule
	public BrowserWebDriverContainer<?> chrome = createChrome();

	public GenericContainer<?> samlContainer;
	
	@After
	public void tearDown() {
		if (samlContainer != null) {
			samlContainer.stop();
		}
	}
	
	@Test
	public void testGetSpMetadata() throws IOException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
		samlContainer.start();
		String metadataUrl = getSpServiceUrl(samlContainer)+"/saml/metadata";

		// When
		ResponseEntity<String> metadataResponse = restTemplate.getForEntity(metadataUrl, String.class);

		// Then
		Assert.assertNotNull(metadataResponse);
	}

	@Test
	public void testAccessProtectedRessouceCorrectUsernamePassword() throws JSONException, JsonMappingException, JsonProcessingException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
		samlContainer.start();
		String username = "testabc";
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();

		// When
		String result = doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);

		// Then
		Assert.assertTrue("Expected to find the start of JSON data", result.indexOf("{") >= 0 );
		Assert.assertTrue("Expected to find the end of JSON data", result.lastIndexOf("}") >= 0 );
		String jsonReturned = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1);
		JsonNode responseParsed = new ObjectMapper().readValue(jsonReturned, JsonNode.class);
		Assert.assertNotNull(responseParsed);
	}

	@Test
	public void testLogout() throws JSONException, JsonMappingException, JsonProcessingException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
		samlContainer.start();
		String username = "testabc"+UUID.randomUUID().toString();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();
		String logoutUrl = "http://"+SAML_SP_URL+"/saml/logout";

		// When
		String afterLoginResult = doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);
		webdriver.get(logoutUrl);
		String afterLogoutResultTitle = webdriver.getTitle();

		// Then
		Assert.assertTrue("Expected to find the start of JSON data", afterLoginResult.indexOf("{") >= 0 );
		Assert.assertTrue("Expected to find the end of JSON data", afterLoginResult.lastIndexOf("}") >= 0 );
		String jsonReturned = afterLoginResult.substring(afterLoginResult.indexOf("{"), afterLoginResult.lastIndexOf("}") + 1);
		JsonNode responseParsed = new ObjectMapper().readValue(jsonReturned, JsonNode.class);
		Assert.assertNotNull(responseParsed);
		Assert.assertEquals("Expected to be returned to the external url of the service...which again should redirect us to the login page", "Log in to test", afterLogoutResultTitle);
	}

	@Test
	public void testLogoutWithLandingPage() throws JSONException, IOException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-logoutlandingpage.config");
		samlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		samlContainer.start();
		String username = "testabc"+UUID.randomUUID().toString();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();
		String logoutUrl = "http://"+SAML_SP_URL+"/saml/logout";

		// When
		doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);
		webdriver.get(logoutUrl);
		String afterLogoutResult = webdriver.getPageSource();

		// Then
		Assert.assertTrue("Expected to be redirected to the pretty logout page", afterLogoutResult.contains("Congratulations with your logout (123456789)!"));
	}
	
	public String getSpServiceUrl(GenericContainer<?> samlContainer) {
		return "http://"+samlContainer.getContainerIpAddress()+":"+samlContainer.getMappedPort(SAML_SP_PORT);
	}
}
