package dk.kvalitetsit.kitcaddy.test;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.http.ResponseEntity;
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

	@Rule
	public GenericContainer<?> samlSp = createSamlSp();

	public static GenericContainer<?> createSamlSp() {
		return 	getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
	}

	@Test
	public void testGetSpMetadata() throws IOException {

		// Given
		String metadataUrl = getSpServiceUrl()+"/saml/metadata";

		// When
		ResponseEntity<String> metadataResponse = restTemplate.getForEntity(metadataUrl, String.class);

		// Then
		Assert.assertNotNull(metadataResponse);
	}

	@Test
	public void testAccessProtectedRessouceCorrectUsernamePassword() throws JSONException, JsonMappingException, JsonProcessingException {

		// Given
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

	public String getSpServiceUrl() {
		return "http://"+samlSp.getContainerIpAddress()+":"+samlSp.getMappedPort(SAML_SP_PORT);
	}
}
