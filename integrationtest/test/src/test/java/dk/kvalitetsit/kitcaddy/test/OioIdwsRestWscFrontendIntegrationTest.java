package dk.kvalitetsit.kitcaddy.test;

import org.junit.Rule;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * 
 *    This testsetup
 * 
 *    | oio idws wsc |    ->  | frontend (SSL-termination) | -- clientcert in 'forwarded-from-nginx' header ->   | oio idws wsp |     ->     | echo service |
 *
 */

public class OioIdwsRestWscFrontendIntegrationTest extends AbstractOioIdwsRestWscIntegrationTest {


	private static final String NGINX_SERVICE_HOST = "frontend";
	private static final int NGINX_SERVICE_PORT = 443;

	private static final String WSC_SERVICE_HOST = "wsc";
	private static final int WSC_SERVICE_PORT = 8686;
	private static final String WSC_SERVICE_URL = WSC_SERVICE_HOST+":"+WSC_SERVICE_PORT;

	private static final String WSP_SERVICE_HOST = "testserviceaa";
	private static final int WSP_SERVICE_PORT = 8787;
	private static final String WSP_SERVICE_URL = WSP_SERVICE_HOST+":"+WSP_SERVICE_PORT;

	
	
	@Rule
	public GenericContainer<?> wsc = createWsc();

	@Rule
	public GenericContainer<?> wsp = createWsp();

	@Rule
	public GenericContainer<?> nginx = createNginx();

	public static GenericContainer<?> createWsc() {
		return 	getKitCaddyContainer(WSC_SERVICE_HOST, WSC_SERVICE_PORT, getDockerNetwork(), "wsc/wsc-functional-frontend.config");
	}

	public static GenericContainer<?> createWsp() {
		return 	getKitCaddyContainer(WSP_SERVICE_HOST, WSP_SERVICE_PORT, getDockerNetwork(), "wsp/wsp-frontend.config");
	}

	public static GenericContainer<?> createNginx() {
		GenericContainer<?> nginxContainer = new GenericContainer<>("nginx:1.19.0")
				.withExposedPorts(NGINX_SERVICE_PORT)
				.withNetwork(getDockerNetwork())

				.withClasspathResourceMapping("frontend/frontend.cer", "/cert/frontend.cer", BindMode.READ_ONLY)
				.withClasspathResourceMapping("frontend/frontend.pem", "/cert/frontend.pem", BindMode.READ_ONLY)
				.withClasspathResourceMapping("frontend/nginx.conf", "/etc/nginx/nginx.conf", BindMode.READ_ONLY)
				.waitingFor(Wait.forListeningPort())	

				.withNetworkAliases(NGINX_SERVICE_HOST);

		return 	nginxContainer;
	}

	@Override
	public String getWscServiceUrl() {
		return "http://"+wsc.getContainerIpAddress()+":"+wsc.getMappedPort(WSC_SERVICE_PORT);
	}

}
