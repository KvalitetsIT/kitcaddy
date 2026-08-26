package dk.kvalitetsit.kitcaddy;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AbstractBrowserBasedIntegrationTest extends AbstractIntegrationTest {

	public String doLoginFlow(WebDriver webdriver, String url, String username, String password) {
		webdriver.get(url);
		webdriver.findElement(By.name("username")).sendKeys(username);
		webdriver.findElement(By.name("password")).sendKeys(password);
		webdriver.findElement(By.name("login")).click();
		return webdriver.getPageSource();
	}
}
