package dk.kvalitetsit.kitcaddy.test;

import java.io.IOException;
import java.util.UUID;

import org.json.JSONException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.client.result.DeleteResult;

import dk.kvalitetsit.kitcaddy.AbstractAllInOneIT;
import dk.kvalitetsit.kitcaddy.TestConstants;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

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
		Expiry expiry = resultBeforeExpiry -> {
            String samlSessionId = resultBeforeExpiry.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.SESSION_HEADER_NAME).asString();
            assertNotNull(samlSessionId, "Expected a session Id");
            Query query = new Query();
            query.addCriteria(Criteria.where(TestConstants.SP_MONGO_SESSION_ID_COLUMN).is(samlSessionId));
            DeleteResult deleteResult = spMongoTemplate.remove(query, TestConstants.SP_MONGO_SESSION_COLLECTION);
            if (!deleteResult.wasAcknowledged() || deleteResult.getDeletedCount() <= 0) {
                fail("No session deleted - test broken :-(");
            }
        };

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry);

		// Then
		String title = responseAfterExpiry.getWebDriver().getTitle();
		assertEquals("Sign in to test", title, "Expected the login page of keycloak after expiry of saml session");
	}

	@Test
	@Disabled
	public void testMongoRestartIsHandledTransparently() throws JSONException, InterruptedException  {
		// Denne test virker, når jeg debugger den
		
		// Given
		Expiry expiry = resultBeforeExpiry -> {
            // This will also remove all data in mongo
            mongoContainer.stop();
            mongoContainer.start();
        };

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry, false); // Do not remove keycloak sessions as we want to "autologin"		
		Thread.sleep(8000); // Let the loaded pages calm down
		
		// Then
		JsonNode responseJsonAfterExpiryParsed = parseJsonReturned(responseAfterExpiry.getWebDriver().getPageSource());
		assertNotNull(responseJsonAfterExpiryParsed, "Expected a json response");


		String wspAuthorizationHeaderBeforeExpiry = responseAfterExpiry.getResponseBeforeExpiry().get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME).stringValue();
		String wspAuthorizationHeaderAfterExpiry = responseJsonAfterExpiryParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME).stringValue();
		assertNotEquals(wspAuthorizationHeaderBeforeExpiry, wspAuthorizationHeaderAfterExpiry, "Expected a new session on the WSP - checking that the authorization header has changed");
	}

	@Test
	public void testMongoConnectionExpiryIsHandledTransparently() throws JSONException  {

		samlSp.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("saml-sp")));
		// Given
		Expiry expiry = resultBeforeExpiry -> {
            try {
                mongoContainer.execInContainer("mongo < /scripts/killallconnections.js");
            } catch (UnsupportedOperationException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry);		

		// Then
		JsonNode responseJsonAfterExpiryParsed = parseJsonReturned(responseAfterExpiry.getWebDriver().getPageSource());
		assertNotNull(responseJsonAfterExpiryParsed, "Expected a json response");


		String wspAuthorizationHeaderBeforeExpiry = responseAfterExpiry.getResponseBeforeExpiry().get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME).stringValue();
		String wspAuthorizationHeaderAfterExpiry = responseJsonAfterExpiryParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME).stringValue();
		assertEquals(wspAuthorizationHeaderBeforeExpiry, wspAuthorizationHeaderAfterExpiry, "Expected the same session on the WSP - checking that the authorization header are the same");
	}


	@Test
	public void testWscSessionExpiryIsHandledTransparently() throws JSONException {

		// Given
		Expiry expiry = resultBeforeExpiry -> {
            // We remove all sessions
            Query query = new Query();
            DeleteResult deleteResult = wscMongoTemplate.remove(query, TestConstants.WSC_MONGO_SESSION_COLLECTION);
            if (!deleteResult.wasAcknowledged() || deleteResult.getDeletedCount() <= 0) {
                fail("No session deleted - test broken :-(");
            }
        };

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry);

		// Then
		JsonNode responseJsonAfterExpiryParsed = parseJsonReturned(responseAfterExpiry.getWebDriver().getPageSource());
		assertNotNull(responseJsonAfterExpiryParsed, "Expected a json response");


		String wspAuthorizationHeaderBeforeExpiry = responseAfterExpiry.getResponseBeforeExpiry().get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME).stringValue();
		String wspAuthorizationHeaderAfterExpiry = responseJsonAfterExpiryParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME).stringValue();
		assertNotEquals("Expected a new session on the WSP - checking that the authorization header has changed", wspAuthorizationHeaderBeforeExpiry, wspAuthorizationHeaderAfterExpiry);
	}

	@Test
	public void testWspSessionExpiryIsHandledTransparently() throws JSONException {

		// Given
		Expiry expiry = resultBeforeExpiry -> {
            // We remove all sessions
            Query query = new Query();
            DeleteResult deleteResult = wspMongoTemplate.remove(query, TestConstants.WSP_MONGO_SESSION_COLLECTION);
            if (!deleteResult.wasAcknowledged() || deleteResult.getDeletedCount() <= 0) {
                fail("No session deleted - test broken :-(");
            }
        };

		// When
		Response responseAfterExpiry = resultAfterExpiry(expiry);		

		// Then
		String responseAfterExpiryBody = responseAfterExpiry.getWebDriver().getPageSource();
		JsonNode responseJsonAfterExpiryParsed = parseJsonReturned(responseAfterExpiryBody);
		assertNotNull(responseJsonAfterExpiryParsed, "Expected a json response");

		String wspAuthorizationHeaderBeforeExpiry = responseAfterExpiry.getResponseBeforeExpiry().get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME).stringValue();
		String wspAuthorizationHeaderAfterExpiry = responseJsonAfterExpiryParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY).get(TestConstants.WSP_AUTHORIZATION_HEADERNAME).stringValue();
		assertNotEquals(wspAuthorizationHeaderBeforeExpiry, wspAuthorizationHeaderAfterExpiry, "Expected a new session on the WSP - checking that the authorization header has changed");
	}


	private Response resultAfterExpiry(Expiry expiry) throws JSONException  {
		return resultAfterExpiry(expiry, true);
	}

	private Response resultAfterExpiry(Expiry expiry, boolean removeKeyCloakCookiesBeforeExpiry) throws JSONException  {
		// Perform login
		String username = UUID.randomUUID().toString();
		String password = "secret1234";
		addUserToKeycloak(username, password);

		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());

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
            return new ObjectMapper().readValue(jsonReturned, JsonNode.class);
		} catch (JacksonException e) {
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
		void doExpiry(JsonNode resultBeforeExpiry);
	}
}
