package dk.kvalitetsit.kitcaddy;

import org.openqa.selenium.remote.RemoteWebDriver;

public class AbstractBrowserBasedIntegrationTest extends AbstractIntegrationTest {

	public String doLoginFlow(RemoteWebDriver webdriver, String url, String username, String password) {
		webdriver.get(url);
		webdriver.findElementByName("username").sendKeys(username);
		webdriver.findElementByName("password").sendKeys(password);
		webdriver.findElementByName("login").click();
		String source = webdriver.getPageSource();
		return source;
	}
}
