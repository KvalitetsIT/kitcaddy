package dk.kvalitetsit.kitcaddy.test;

import org.json.JSONException;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.http.ResponseEntity;

import dk.kvalitetsit.kitcaddy.AbstractIntegrationTest;

public class SamlServiceProviderIntegrationTest extends AbstractIntegrationTest {

	
	@Test
	public void testGetSpMetadata() {
		
		// Given
		String metadataUrl = getSpServiceUrl()+"/saml/metadata";
		
		// When
		ResponseEntity<String> response = restTemplate.getForEntity(metadataUrl, String.class);

//		Timer timer;
	//	MetadataProvider httpMetadataProvider = new ResourceBackedMetadataProvider(timer, resource);
	//	ExtendedMetadataDelegate emd = new ExtendedMetadataDelegate(httpMetadataProvider);
		
		// Then
//		emd.getEntitiesDescriptor(name);
	}
	
	@Test
	public void testAccessProtectedRessouceCorrectUsernamePassword() throws JSONException {
		
		// Given
		String username = "testabc";
		String password = "secret1234";
		addUser(username, password);
		
		RemoteWebDriver webdriver = getRemoteWebDriver();
		
		// When
		String result = doLoginFlow(webdriver, username, password);
		
		// Then
		
		System.out.println(result);
	}
	
	
	public String doLoginFlow(RemoteWebDriver webdriver, String username, String password) {
		return doLoginFlow(webdriver, "http://"+UISERVICE_URL+"/echo/test", username, password);
	}
	
	public String doLoginFlow(RemoteWebDriver webdriver, String url, String username, String password) {
		webdriver.get(url);
		webdriver.findElementByName("username").sendKeys(username);
		webdriver.findElementByName("password").sendKeys(password);
		webdriver.findElementByName("login").click();;

		String source = webdriver.getPageSource();
		
		return source;
	}

}
