package dk.kvalitetsit.kitcaddy.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.UUID;

import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.selenium.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.kvalitetsit.kitcaddy.AbstractBrowserBasedIntegrationTest;
import dk.kvalitetsit.kitcaddy.TestConstants;
import org.testcontainers.junit.jupiter.Container;
import tools.jackson.databind.node.StringNode;

/**
 * 
 *    This testsetup
 *
 *    | Webbrowser |    ->    | SAML-SP |    ->    | echoservice | 
 *    
 *                      ->    | otherSAML-SP |    ->    | echoservice | 
 *
 */
@Testcontainers
public class SamlServiceProviderIntegrationTest extends AbstractBrowserBasedIntegrationTest {

	public static final String 	SAML_SP_HOST 	= "uiservice";
	public static final int 	SAML_SP_PORT 	= 8787;
	public static final String 	SAML_SP_URL 	= SAML_SP_HOST+":"+SAML_SP_PORT;

	public static final String 	OTHER_SAML_SP_HOST 	= "other";
	public static final int 	OTHER_SAML_SP_PORT 	= 8787;
	public static final String 	OTHER_SAML_SP_URL 	= OTHER_SAML_SP_HOST+":"+OTHER_SAML_SP_PORT;

	public static final String 	ENCRYPTED_SAML_SP_HOST 	= "encrypted";
	public static final int 	ENCRYPTED_SAML_SP_PORT 	= 8787;
	public static final String 	ENCRYPTED_SP_URL 	= ENCRYPTED_SAML_SP_HOST+":"+ENCRYPTED_SAML_SP_PORT;

	@Container
	public BrowserWebDriverContainer chrome = createChrome();

	public GenericContainer<?> samlContainer;
	public GenericContainer<?> otherSamlContainer;
	public GenericContainer<?> encryptedSamlContainer;
	
	@AfterEach
	public void tearDown() {
		if (samlContainer != null) {
			samlContainer.stop();
		}
	}

	@AfterEach
	public void tearDownOther() {
		if (otherSamlContainer != null) {
			otherSamlContainer.stop();
		}
	}

	@AfterEach
	public void tearDownEncrypted() {
		if (encryptedSamlContainer != null) {
			encryptedSamlContainer.stop();
		}
	}

	@Test
	public void testGetSpMetadata() {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
		samlContainer.start();
		String metadataUrl = getSpServiceUrl(samlContainer)+"/saml/metadata"; // check that Kitcaddy server

		// When
		ResponseEntity<String> metadataResponse = restTemplate.getForEntity(metadataUrl, String.class);

		// Then
		assertNotNull(metadataResponse);
	}

	@Test
	public void testAccessProtectedRessouceCorrectUsernamePassword() throws JSONException, JacksonException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
		samlContainer.start();
		String username = "testabc"+ UUID.randomUUID();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());

		// When
		String result = doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);

		// Then
		assertTrue(result.indexOf("{") >= 0, "Expected to find the start of JSON data");
		assertTrue(result.lastIndexOf("}") >= 0, "Expected to find the end of JSON data");
		String jsonReturned = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1);
		JsonNode responseParsed = new ObjectMapper().readValue(jsonReturned, StringNode.class);
		assertNotNull(responseParsed);
	}

	@Test
	public void testGetSessionDataOnUrlRessouceCorrectUsernamePassword() throws JSONException, JacksonException, RestClientException, URISyntaxException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
		samlContainer.start();
		String username = "testabc"+UUID.randomUUID();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());

		// We don't care about the return value, we only care about the cookie
		doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);
		Cookie cookie = webdriver.manage().getCookieNamed(TestConstants.SESSION_HEADER_NAME);
		assertNotNull(cookie);
		String sessionId = cookie.getValue();

		// When
		HttpHeaders headers = new HttpHeaders();
		headers.add(TestConstants.SESSION_HEADER_NAME, sessionId);
		RestTemplate rt = new RestTemplate();
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<String> response = rt.exchange(new URI(getSpServiceUrl(samlContainer)+"/getsessiondata"), HttpMethod.GET, requestEntity, String.class);
		
		// Then
		assertNotNull(response);
		JsonNode responseParsed = new ObjectMapper().readValue(response.getBody(), StringNode.class);
		assertNotNull(responseParsed);
		String authenticationTokenValue = responseParsed.get(TestConstants.SESSION_DATA_KEY_AUTHENTICATION_TOKEN).stringValue();
		assertNotNull(authenticationTokenValue);
		String decodedAuthenticationToken = new String(Base64.getDecoder().decode(authenticationTokenValue.getBytes()));
		assertNotNull(decodedAuthenticationToken);
	}

	@Test
	public void testLogout() throws JSONException, JacksonException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml.config");
		samlContainer.start();
		String username = "testabc"+ UUID.randomUUID();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
		String logoutUrl = "http://"+SAML_SP_URL+"/saml/logout";

		// When
		String afterLoginResult = doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);
		webdriver.get(logoutUrl);
		String afterLogoutResultTitle = webdriver.getTitle();

		// Then
		assertTrue(afterLoginResult.indexOf("{") >= 0, "Expected to find the start of JSON data");
		assertTrue(afterLoginResult.lastIndexOf("}") >= 0, "Expected to find the end of JSON data");
		String jsonReturned = afterLoginResult.substring(afterLoginResult.indexOf("{"), afterLoginResult.lastIndexOf("}") + 1);
		JsonNode responseParsed = new ObjectMapper().readValue(jsonReturned, StringNode.class);
		assertNotNull(responseParsed);
		assertEquals("Sign in to test", afterLogoutResultTitle, "Expected to be returned to the external url of the service...which again should redirect us to the login page");
	}

	@Test
	public void testLogoutWithLandingPage() throws JSONException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-logoutlandingpage.config");
		samlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		samlContainer.start();
		String username = "testabc"+ UUID.randomUUID();
		String password = "secret1234";
		addUserToKeycloak(username, password);

		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());

		String logoutUrl = "http://"+SAML_SP_URL+"/saml/logout";

		// When
		doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/echo/test", username, password);
		webdriver.get(logoutUrl);
		String afterLogoutResult = webdriver.getPageSource();

		// Then
		assertNotNull(afterLogoutResult, "Expected to be redirected to the pretty logout page");
		assertTrue(afterLogoutResult.contains("Congratulations with your logout (123456789)!"), "Expected to be redirected to the pretty logout page");
	}

	@Test
	public void testSLOWithLandingPage() throws JSONException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-logoutlandingpage.config");
		samlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		samlContainer.start();

		otherSamlContainer = getKitCaddyContainer(OTHER_SAML_SP_HOST, OTHER_SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-other.config");
		otherSamlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		otherSamlContainer.start();

		String username = "testabc"+ UUID.randomUUID();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
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
		assertTrue(afterLogoutResult.contains("Congratulations with your logout (123456789)!"), "Expected to be redirected to the pretty logout page");
		assertTrue(afterSingleSignOnHopefully.contains("\"host\": \"other:8787\""), "Single Logon to other app failed");
		assertTrue(otherAfterSlo.contains("<title>Sign in to test</title>"), "SLO failed for othercontainer");
	}

	@Test
	public void testLoginWithEncryptedAssertion() throws JSONException {

		// Given
		encryptedSamlContainer = getKitCaddyContainer(ENCRYPTED_SAML_SP_HOST, ENCRYPTED_SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-encrypted.config");
		encryptedSamlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		encryptedSamlContainer.start();

		String username = "testabc"+ UUID.randomUUID();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());

		// When
		String result = doLoginFlow(webdriver, "http://"+ENCRYPTED_SP_URL+"/echo/test", username, password);

		// Then
		assertTrue(result.indexOf("{") >= 0, "Expected to find the start of JSON data");
		assertTrue(result.lastIndexOf("}") >= 0, "Expected to find the end of JSON data");
		String jsonReturned = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1);
		JsonNode responseParsed = new ObjectMapper().readValue(jsonReturned, StringNode.class);
		assertNotNull(responseParsed);

		JsonNode headerJson = responseParsed.get("headers");
		assertNotNull(headerJson);

		JsonNode sessionDataHeaderJson = headerJson.get("sessiondataheader");
		assertNotNull(sessionDataHeaderJson);
		
		String sessionDataHeaderContent = sessionDataHeaderJson.asString();
		assertNotNull(sessionDataHeaderContent);
		
		String decodedData = new String(Base64.getDecoder().decode(sessionDataHeaderContent));
		JsonNode decodedDataJson = new ObjectMapper().readValue(decodedData, StringNode.class);
		assertNotNull(decodedDataJson);
		
		JsonNode authenticationTokenJson = decodedDataJson.get("Authenticationtoken");
		assertNotNull(authenticationTokenJson);
		String authenticationToken = authenticationTokenJson.asString();
		String decodedAuthenticationToken = new String(Base64.getDecoder().decode(authenticationToken));
		assertNotNull(decodedAuthenticationToken);
		assertTrue(decodedAuthenticationToken.startsWith("<saml:Assertion"));
		assertTrue(decodedAuthenticationToken.contains(username));
	}

	
	
	@Test
	public void testSLOWithLandingPageLogoutInitiatedByOther() throws JSONException {

		// Given
		samlContainer = getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-logoutlandingpage.config");
		samlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		samlContainer.start();

		otherSamlContainer = getKitCaddyContainer(OTHER_SAML_SP_HOST, OTHER_SAML_SP_PORT, getDockerNetwork(), "samlserviceprovider/saml-other.config");
		otherSamlContainer.withClasspathResourceMapping("samlserviceprovider/pretty-logoutpage.html", "/htmls/pretty-logoutpage.html", BindMode.READ_ONLY);
		otherSamlContainer.start();

		String username = "testslocba"+ UUID.randomUUID();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
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
		assertTrue(afterLogoutResult.contains("<title>Sign in to test</title>"), "Expected to be redirected to login page");
		assertTrue(afterSingleSignOnHopefully.contains("\"host\": \"other:8787\""), "Single Logon to other app failed");
		assertTrue(otherAfterSlo.contains("<title>Sign in to test</title>"), "SLO failed for saml container");
	}

	public String getSpServiceUrl(GenericContainer<?> samlContainer) {
		return "http://"+ samlContainer.getHost() +":"+samlContainer.getMappedPort(SAML_SP_PORT);
	}
}
