package dk.kvalitetsit.kitcaddy.test;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.kvalitetsit.kitcaddy.AbstractAllInOneIT;


public class AllInOneFunctionalIT extends AbstractAllInOneIT {

	
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
}
