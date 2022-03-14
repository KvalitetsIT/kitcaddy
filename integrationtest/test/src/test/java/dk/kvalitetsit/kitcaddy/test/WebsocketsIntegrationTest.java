package dk.kvalitetsit.kitcaddy.test;

import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Sleeper;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dk.kvalitetsit.kitcaddy.AbstractBrowserBasedIntegrationTest;

/**
 * 
 *    This testsetup
 * 
 *    | Webbrowser |    ->    | SAML-SP |    ->    | websocketservice |      ->     | echo.websocket.org |
 *
 *
 * @see https://www.websocket.org/echo.html
 */
public class WebsocketsIntegrationTest extends AbstractBrowserBasedIntegrationTest {

	public static final String 	SAML_SP_HOST 	= "uiservice";
	public static final int 	SAML_SP_PORT 	= 8787;
	public static final String 	SAML_SP_URL 	= SAML_SP_HOST+":"+SAML_SP_PORT;

	public static final String 	WEBSOCKETSERVICE_HOST	 	= "websocketservice";
	public static final int 	WEBSOCKETSERVICE_PORT	 	= 8585;
	public static final String 	WEBSOCKETSERVICE_URL 		= WEBSOCKETSERVICE_HOST+":"+WEBSOCKETSERVICE_PORT;

	@Rule
	public BrowserWebDriverContainer<?> chrome = createChrome();

	@Rule
	public GenericContainer<?> samlSp = createSamlSp();

	@Rule
	public GenericContainer<?> wssService = createWssService();

	public static GenericContainer<?> createSamlSp() {
		return getKitCaddyContainer(SAML_SP_HOST, SAML_SP_PORT, getDockerNetwork(), "websockets/saml.config");
	}

	public GenericContainer<?> createWssService() {
		GenericContainer<?> wssContainer = getKitCaddyContainer(WEBSOCKETSERVICE_HOST, WEBSOCKETSERVICE_PORT, getDockerNetwork(), "websockets/websockets.config");
		wssContainer.withClasspathResourceMapping("websockets/websocket.html", "/htmls/websocket.html", BindMode.READ_ONLY);
		return wssContainer;
	}

	@Test
	public void testAccessProtectedWssService() throws JSONException, JsonMappingException, JsonProcessingException, InterruptedException {

		// Given
		String username = "websocket"+UUID.randomUUID().toString();
		String password = "secret1234";
		addUserToKeycloak(username, password);
		RemoteWebDriver webdriver = chrome.getWebDriver();

		// When
		String url = "http://"+SAML_SP_URL+"/websocket.html";
		doLoginFlow(webdriver, url, username, password);
		
		// 	Then
		// 	Thread.sleep(2000); // Async javascript ahead...wait a bit :-)
		//	String source = webdriver.getPageSource();
		//Thread.sleep(5000); // Async javascript ahead...wait a bit :-)
		List<WebElement> outputs = webdriver.findElementById("output").findElements(By.xpath("//*"));
		String outputText = outputs.get(0).getText();
		//	Thread.sleep(2000); // Async javascript ahead...wait a bit :-)
		System.out.println("TEST THIS HERE 2");
		System.out.println(outputText);
		System.out.println("TEST THIS HERE 2");
		Assert.assertEquals("Expected response from the websocket server", "WebSocket Test\nCONNECTED\nSENT: WebSocket rocks\nRESPONSE: WebSocket rocks\nDISCONNECTED", outputText);
		Thread.sleep(5000);
	}
}