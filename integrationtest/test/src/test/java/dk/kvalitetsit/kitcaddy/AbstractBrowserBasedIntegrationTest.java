package dk.kvalitetsit.kitcaddy;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

public class AbstractBrowserBasedIntegrationTest extends AbstractIntegrationTest {

	public String doLoginFlow(RemoteWebDriver webdriver, String url, String username, String password) {
		webdriver.get(url);
		webdriver.findElement(By.name("username")).sendKeys(username);
		webdriver.findElement(By.name("password")).sendKeys(password);
		webdriver.findElement(By.name("login")).click();
		String source = webdriver.getPageSource();
		return source;
	}
}
