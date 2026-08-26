package dk.kvalitetsit.kitcaddy.test;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import dk.kvalitetsit.kitcaddy.AbstractAllInOneIT;
import tools.jackson.databind.node.StringNode;


public class AllInOneFunctionalIntegrationTest extends AbstractAllInOneIT {

	
	@Test
	public void testAccessProtectedRessouceCorrectUsernamePassword() throws JSONException, JacksonException {
		
		// Given
		String username = "test123";
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
		
		// When
		String result = doLoginFlow(webdriver, "http://"+SAML_SP_URL+"/service/test", username, password);
		
		// Then
		assertTrue(result.indexOf("{") >= 0 , "Expected to find the start of JSON data");
		assertTrue(result.lastIndexOf("}") >= 0, "Expected to find the end of JSON data");
		String jsonReturned = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1);
		JsonNode responseParsed = new ObjectMapper().readValue(jsonReturned, StringNode.class);
		assertNotNull(responseParsed);
	}
}
