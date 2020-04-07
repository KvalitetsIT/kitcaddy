package dk.kvalitetsit.kitcaddy.test;

import org.json.JSONException;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

public class AllInOneIntegrationTest extends SamlServiceProviderIntegrationTest {

	
	@Test
	public void testAccessProtectedRessouceCorrectUsernamePassword() throws JSONException {
		
		// Given
		String username = "test123";
		String password = "secret1234";
		addUser(username, password);
		
		RemoteWebDriver webdriver = getRemoteWebDriver();
		
		// When
		String result = doLoginFlow(webdriver, "http://"+UISERVICE_URL+"/service/test", username, password);
		
		// Then
		
		System.out.println("kuk");
	}

}
