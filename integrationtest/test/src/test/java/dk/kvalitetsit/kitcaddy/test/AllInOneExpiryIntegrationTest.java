package dk.kvalitetsit.kitcaddy.test;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Predicate;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.wait.strategy.Wait;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.mongodb.client.result.DeleteResult;

import dk.kvalitetsit.kitcaddy.AbstractAllInOneIT;
import dk.kvalitetsit.kitcaddy.TestConstants;

public class AllInOneExpiryIntegrationTest extends AbstractAllInOneIT {

	private static final String TEST_URL = "http://"+SAML_SP_URL+"/service/test";

	@Autowired
	@Qualifier("spMongoTemplate")
	MongoTemplate spMongoTemplate;

	@Autowired
	@Qualifier("wscMongoTemplate")
	MongoTemplate wscMongoTemplate;

	@Autowired
	@Qualifier("wspMongoTemplate")
	MongoTemplate wspMongoTemplate;

	@Test
	public void testSamlSpSessionExpiryTriggersNewLogin() throws JSONException  {

		// Given
		Expiry expiry = new Expiry() {
			@Override
			public void doExpiry(JsonNode resultBeforeExpiry) {
				String samlSessionId = resultBeforeExpiry.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.SESSION_HEADER_NAME).asText();
				Assert.assertNotNull("Expected a session Id", samlSessionId);
				Query query = new Query();
				query.addCriteria(Criteria.where(TestConstants.SP_MONGO_SESSION_ID_COLUMN).is(samlSessionId));;
				DeleteResult deleteResult = spMongoTemplate.remove(query, TestConstants.SP_MONGO_SESSION_COLLECTION);
				if (!deleteResult.wasAcknowledged() || deleteResult.getDeletedCount() <= 0) {
					Assert.assertTrue("No session deleted - test broken :-(", false);
				}
			}
		};

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry);

		// Then
		String title = responseAfterExpiry.getWebDriver().getTitle();
		Assert.assertEquals("Expected the login page of keycloak after expiry of saml session", "Sign in to test", title);
	}

	@Test
	@Ignore
	public void testMongoRestartIsHandledTransparently() throws JSONException, InterruptedException  {
		// Denne test virker, når jeg debugger den
		
		// Given
		Expiry expiry = new Expiry() {
			@Override
			public void doExpiry(JsonNode resultBeforeExpiry) {
				// This will also remove all data in mongo
				mongoContainer.stop();
				mongoContainer.start();
			}
		};

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry, false); // Do not remove keycloak sessions as we want to "autologin"		
		Thread.sleep(8000); // Let the loaded pages calm down
		
		// Then
		JsonNode responseJsonAfterExpiryParsed = parseJsonReturned(responseAfterExpiry.getWebDriver().getPageSource());
		Assert.assertNotNull("Expected a json response", responseJsonAfterExpiryParsed);


		String wspAuthorizationHeaderBeforeExpiry = ((TextNode) responseAfterExpiry.getResponseBeforeExpiry().get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME)).textValue();
		String wspAuthorizationHeaderAfterExpiry = ((TextNode) responseJsonAfterExpiryParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME)).textValue();
		Assert.assertNotEquals("Expected a new session on the WSP - checking that the authorization header has changed", wspAuthorizationHeaderBeforeExpiry, wspAuthorizationHeaderAfterExpiry);
	}

	@Test
	public void testMongoConnectionExpiryIsHandledTransparently() throws JSONException  {

		// Given
		Expiry expiry = new Expiry() {
			@Override
			public void doExpiry(JsonNode resultBeforeExpiry) {
				try {
					mongoContainer.execInContainer("mongo < /scripts/killallconnections.js");
				} catch (UnsupportedOperationException | IOException | InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry);		

		// Then
		JsonNode responseJsonAfterExpiryParsed = parseJsonReturned(responseAfterExpiry.getWebDriver().getPageSource());
		Assert.assertNotNull("Expected a json response", responseJsonAfterExpiryParsed);


		String wspAuthorizationHeaderBeforeExpiry = ((TextNode) responseAfterExpiry.getResponseBeforeExpiry().get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME)).textValue();
		String wspAuthorizationHeaderAfterExpiry = ((TextNode) responseJsonAfterExpiryParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME)).textValue();
		Assert.assertEquals("Expected the same session on the WSP - checking that the authorization header are the same", wspAuthorizationHeaderBeforeExpiry, wspAuthorizationHeaderAfterExpiry);
	}


	@Test
	public void testWscSessionExpiryIsHandledTransparently() throws JSONException {

		// Given
		Expiry expiry = new Expiry() {
			@Override
			public void doExpiry(JsonNode resultBeforeExpiry) {
				// We remove all sessions
				Query query = new Query();
				DeleteResult deleteResult = wscMongoTemplate.remove(query, TestConstants.WSC_MONGO_SESSION_COLLECTION);
				if (!deleteResult.wasAcknowledged() || deleteResult.getDeletedCount() <= 0) {
					Assert.assertTrue("No session deleted - test broken :-(", false);
				}
			}
		};

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry);		

		// Then
		JsonNode responseJsonAfterExpiryParsed = parseJsonReturned(responseAfterExpiry.getWebDriver().getPageSource());
		Assert.assertNotNull("Expected a json response", responseJsonAfterExpiryParsed);


		String wspAuthorizationHeaderBeforeExpiry = ((TextNode) responseAfterExpiry.getResponseBeforeExpiry().get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME)).textValue();
		String wspAuthorizationHeaderAfterExpiry = ((TextNode) responseJsonAfterExpiryParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME)).textValue();
		Assert.assertNotEquals("Expected a new session on the WSP - checking that the authorization header has changed", wspAuthorizationHeaderBeforeExpiry, wspAuthorizationHeaderAfterExpiry);
	}

	@Test
	public void testWspSessionExpiryIsHandledTransparently() throws JSONException {

		// Given
		Expiry expiry = new Expiry() {
			@Override
			public void doExpiry(JsonNode resultBeforeExpiry) {
				// We remove all sessions
				Query query = new Query();
				DeleteResult deleteResult = wspMongoTemplate.remove(query, TestConstants.WSP_MONGO_SESSION_COLLECTION);
				if (!deleteResult.wasAcknowledged() || deleteResult.getDeletedCount() <= 0) {
					Assert.assertTrue("No session deleted - test broken :-(", false);
				}
			}
		};

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry);		

		// Then
		String responseAfterExpiryBody = responseAfterExpiry.getWebDriver().getPageSource();
		JsonNode responseJsonAfterExpiryParsed = parseJsonReturned(responseAfterExpiryBody);
		Assert.assertNotNull("Expected a json response", responseJsonAfterExpiryParsed);

		String wspAuthorizationHeaderBeforeExpiry = ((TextNode) responseAfterExpiry.getResponseBeforeExpiry().get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME)).textValue();
		String wspAuthorizationHeaderAfterExpiry = ((TextNode) responseJsonAfterExpiryParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME)).textValue();
		Assert.assertNotEquals("Expected a new session on the WSP - checking that the authorization header has changed", wspAuthorizationHeaderBeforeExpiry, wspAuthorizationHeaderAfterExpiry);
	}


	private Response  resultAfterExpiry(Expiry expiry) throws JSONException  {
		return resultAfterExpiry(expiry, true);
	}

	private Response resultAfterExpiry(Expiry expiry, boolean removeKeyCloakCookiesBeforeExpiry) throws JSONException  {
		// Perform login
		String username = UUID.randomUUID().toString();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();
		String resultBeforeExpiry = doLoginFlow(webdriver, TEST_URL, username, password);

		// Access protected ressource
		JsonNode responseParsed = parseJsonReturned(resultBeforeExpiry);

		if (removeKeyCloakCookiesBeforeExpiry) {
			// Make sure that we don't log automatically into keycloak on session expiry
			webdriver.get(TestConstants.KEYCLOAK_ACCOUNT_URL);
			webdriver.getPageSource();
			webdriver.manage().deleteAllCookies();
		}

		// Expire session
		expiry.doExpiry(responseParsed);

		// Access protected resource
		webdriver.get(TEST_URL);

		return new Response(responseParsed, webdriver);
	}

	private JsonNode parseJsonReturned(String result) {

		if (!result.contains("{") || !result.contains("}")) {
			System.out.println("Not parsed: "+result);
			return null;
		}

		String jsonReturned = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1);
		try {
			JsonNode responseParsed = new ObjectMapper().readValue(jsonReturned, JsonNode.class);
			return responseParsed;
		} catch (JsonProcessingException e) {
				System.out.println("Not parsed exception: "+result);
			return null;
		}
	}


	private static class Response {

		RemoteWebDriver webDriver;

		JsonNode responseBeforeExpiry;

		public Response(JsonNode responseBeforeExpiry, RemoteWebDriver webDriver) {
			this.responseBeforeExpiry = responseBeforeExpiry;
			this.webDriver = webDriver;
		}

		public RemoteWebDriver getWebDriver() {
			return webDriver;
		}

		public JsonNode getResponseBeforeExpiry() {
			return responseBeforeExpiry;
		}
	}

	private interface Expiry {
		public void doExpiry(JsonNode resultBeforeExpiry);
	}
}
