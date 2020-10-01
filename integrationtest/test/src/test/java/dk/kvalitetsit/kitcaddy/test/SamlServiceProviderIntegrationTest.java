package dk.kvalitetsit.kitcaddy.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.UUID;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import dk.kvalitetsit.kitcaddy.AbstractBrowserBasedIntegrationTest;
import dk.kvalitetsit.kitcaddy.TestConstants;

/**
 * 
 *    This testsetup
 * 
 *    | Webbrowser |    ->    | SAML-SP |    ->    | echoservice | 
 *    
 *                      ->    | otherSAML-SP |    ->    | echoservice | 
 *
 */
public class SamlServiceProviderIntegrationTest extends AbstractBrowserBasedIntegrationTest {

	public static final String 	SAML_SP_HOST 	= "uiservice";
	public static final int 	SAML_SP_PORT 	= 8787;
	public static final String 	SAML_SP_URL 	= SAML_SP_HOST+":"+SAML_SP_PORT;

	public static final String 	OTHER_SAML_SP_HOST 	= "other";
	public static final int 	OTHER_SAML_SP_PORT 	= 8787;
	public static final String 	OTHER_SAML_SP_URL 	= OTHER_SAML_SP_HOST+":"+OTHER_SAML_SP_PORT;

	@Rule
	public BrowserWebDriverContainer<?> chrome = createChrome();

	public GenericContainer<?> samlContainer;
	public GenericContainer<?> otherSamlContainer;
	
	@After
	public void tearDown() {
		if (samlContainer != null) {
			samlContainer.stop();
		}
	}

	@After
	public void tearDownOther() {
		if (otherSamlContainer != null) {
			otherSamlContainer.stop();
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
		String username = "testabc"+UUID.randomUUID().toString();
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
	public void testGetSessionDataOnUrlRessouceCorrectUsernamePassword() throws JSONException, JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
		samlContainer.start();
		String username = "testabc"+UUID.randomUUID().toString();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();
		String result = doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);
		Cookie cookie = webdriver.manage().getCookieNamed(TestConstants.SESSION_HEADER_NAME);
		String sessionId = cookie.getValue();

		// When
		HttpHeaders headers = new HttpHeaders();
		headers.add(TestConstants.SESSION_HEADER_NAME, sessionId);
		RestTemplate rt = new RestTemplate();
		HttpEntity<Void> requestEntity = new HttpEntity<Void>(headers);
		ResponseEntity<String> response = rt.exchange(new URI(getSpServiceUrl(samlContainer)+"/getsessiondata"), HttpMethod.GET, requestEntity, String.class);
		
		// Then
		Assert.assertNotNull(response);
		JsonNode responseParsed = new ObjectMapper().readValue(response.getBody(), JsonNode.class);
		Assert.assertNotNull(responseParsed);
		String authenticationTokenValue = ((TextNode) responseParsed.get(TestConstants.SESSION_DATA_KEY_AUTHENTICATION_TOKEN)).textValue();
		Assert.assertNotNull(authenticationTokenValue);
		String decodedAuthenticationToken = new String(Base64.getDecoder().decode(authenticationTokenValue.getBytes()));
		Assert.assertNotNull(decodedAuthenticationToken);
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

	@Test
	public void testSLOWithLandingPage() throws JSONException, IOException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-logoutlandingpage.config");
		samlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		samlContainer.start();

		otherSamlContainer = getKitCaddyContainer(OTHER_SAML_SP_HOST, OTHER_SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-other.config");
		otherSamlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		otherSamlContainer.start();

		String username = "testabc"+UUID.randomUUID().toString();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();
		String otherUrl = "http://"+OTHER_SAML_SP_URL+"/echo/test";
		String logoutUrl = "http://"+SAML_SP_URL+"/saml/logout";

		// When
		doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);
		
		webdriver.get(otherUrl);
		String afterSingleSignOnHopefully = webdriver.getPageSource();
		
		webdriver.get(logoutUrl);
		String afterLogoutResult = webdriver.getPageSource();
		
		webdriver.get(otherUrl);
		String otherAfterSlo = webdriver.getPageSource();

		// Then
		Assert.assertTrue("Expected to be redirected to the pretty logout page", afterLogoutResult.contains("Congratulations with your logout (123456789)!"));
		Assert.assertTrue("Single Logon to other app failed", afterSingleSignOnHopefully.contains("\"host\": \"other:8787\""));
		Assert.assertTrue("SLO failed for othercontainer", otherAfterSlo.contains("<title>Log in to test</title>"));
	}

	@Test
	public void testSLOWithLandingPageLogoutInitiatedByOther() throws JSONException, IOException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-logoutlandingpage.config");
		samlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		samlContainer.start();

		otherSamlContainer = getKitCaddyContainer(OTHER_SAML_SP_HOST, OTHER_SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-other.config");
		otherSamlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		otherSamlContainer.start();

		String username = "testslocba"+UUID.randomUUID().toString();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();
		String otherUrl = "http://"+OTHER_SAML_SP_URL+"/echo/test";
		String logoutUrl = "http://"+OTHER_SAML_SP_URL+"/saml/logout";
		String testUrl = "http://"+SAML_SP_URL+"/echo/test";

		// When
		doLoginFlow(webdriver, testUrl, username, password);
		
		webdriver.get(otherUrl);
		String afterSingleSignOnHopefully = webdriver.getPageSource();
		
		webdriver.get(logoutUrl);
		String afterLogoutResult = webdriver.getPageSource();
		
		webdriver.get(testUrl);
		String otherAfterSlo = webdriver.getPageSource();

		// Then
		Assert.assertTrue("Expected to be redirected to login page", afterLogoutResult.contains("<title>Log in to test</title>"));
		Assert.assertTrue("Single Logon to other app failed", afterSingleSignOnHopefully.contains("\"host\": \"other:8787\""));
		Assert.assertTrue("SLO failed for saml container", otherAfterSlo.contains("<title>Log in to test</title>"));
	}

	public String getSpServiceUrl(GenericContainer<?> samlContainer) {
		return "http://"+samlContainer.getContainerIpAddress()+":"+samlContainer.getMappedPort(SAML_SP_PORT);
	}
}
