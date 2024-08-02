package dk.kvalitetsit.kitcaddy;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import dk.kvalitetsit.kitcaddy.keycloak.Credential;
import dk.kvalitetsit.kitcaddy.keycloak.User;

public class AbstractIntegrationTest {

	private static Logger keycloakLogger = LoggerFactory.getLogger("keycloak-logger");

	protected RestTemplate restTemplate = new RestTemplate();

	private static Network n;

	public static Integer keycloakPort;
	private static String keycloackHost;

	public static Integer mongoPort;
	public static String mongoHost;
	
	protected static GenericContainer<?> mongoContainer;
	
	protected static Network getDockerNetwork() {
		return n;
	}

	protected static BrowserWebDriverContainer<?> createChrome() {
		return (BrowserWebDriverContainer<?>) new BrowserWebDriverContainer<>().withCapabilities(new ChromeOptions()).withNetwork(n);
	}

	protected final static String getKitCaddyVersionUnderTest() {
		return "dev";//"1.3.19";
	}

	@BeforeClass
	public static void setupTestEnvironment() throws UnsupportedOperationException, IOException, InterruptedException {

		if (n == null) {

			n = Network.newNetwork();

			// Start the Mongo container
			String mongoAlias = "mongo";
			mongoContainer = new GenericContainer<>("mongo:7.0")
					.withExposedPorts(27017)
					.withNetwork(n)
					.withReuse(false)
					.withClasspathResourceMapping("mongo/killallconnections.js", "/scripts/killallconnections.js", BindMode.READ_ONLY)
					.waitingFor(Wait.forListeningPort())
					.withNetworkAliases(mongoAlias);
			mongoContainer.start();
			mongoPort = mongoContainer.getMappedPort(27017);
			mongoHost = mongoContainer.getContainerIpAddress();

			// Start Keycloack service
			File keycloakCertificate = getKeycloakContainer(n);

			// Start STS
			GenericContainer<?> stsBackend = new GenericContainer<>("kvalitetsit/sts:1.0.34")
					.withEnv("LOG_lEVEL", "INFO")

					.withEnv("STS_ISSUER", "sts")
					.withEnv("STS_TOKEN_LIFETIME", "2800")
					.withEnv("STS_SUPPORTED_CLAIMS", TestConstants.STS_ALLOWED_CLAIM_A+","+TestConstants.STS_ALLOWED_CLAIM_B)
					.withEnv("STS_COPY_ATTRIBUTES", "claim-a")
					.withEnv("USE_DB", "false")
					.withEnv("STS_TOKEN_VALID_FROM_OFFSET", "30")

					// STS keys
					.withClasspathResourceMapping("sts/sts.cer", "/certificates/sts.cer", BindMode.READ_ONLY)
					.withClasspathResourceMapping("sts/sts.pem", "/certificates/sts.pem", BindMode.READ_ONLY)
					.withEnv("STS_CERTIFICATE", "/certificates/sts.cer")
					.withEnv("STS_KEY", "/certificates/sts.pem")

					// Trust
					.withFileSystemBind(keycloakCertificate.getAbsolutePath(), "/trust/keycloak.cer")
					.withEnv("STS_TRUST_CA_PATH", "/trust/*")

					// Clients
					.withClasspathResourceMapping("sts/clients.json", "/clients/clients.json", BindMode.READ_ONLY)
					.withClasspathResourceMapping("wsc/wsc.cer", "/clients/wsc.cer", BindMode.READ_ONLY)
					.withEnv("JSON_CLIENT_PATH", "/clients/clients.json")

					.withExposedPorts(8181)
					.withNetwork(n)
					.waitingFor(Wait.forListeningPort())
					.withNetworkAliases("sts");
			stsBackend.start();

			// Echo Service (b:devackend service for test)
			GenericContainer<?> echoService = new GenericContainer<>("mendhak/http-https-echo:31")
					.withExposedPorts(8080)
					.withNetwork(n)
					.waitingFor(Wait.forListeningPort())	
					.withNetworkAliases("echo");
			echoService.start();

		}
	}

	public static GenericContainer<?> getKitCaddyContainer(String alias, int port, Network n, String config) {
		GenericContainer kitCaddy = getKitCaddyContainer("kvalitetsit/kitcaddy:"+getKitCaddyVersionUnderTest(), alias, port, n, config);
		return kitCaddy;
	}

	private static GenericContainer<?> getKitCaddyContainer(String image, String alias, int port, Network n, String config) {
		GenericContainer<?> kitCaddyContainer = new GenericContainer<>(image)
				.withExposedPorts(port)
				.withNetwork(n)

				.withClasspathResourceMapping("samlserviceprovider/sp.cer", "/sp/sp.cer", BindMode.READ_ONLY)
				.withClasspathResourceMapping("samlserviceprovider/sp.pem", "/sp/sp.pem", BindMode.READ_ONLY)

				.withClasspathResourceMapping("wsc/wsc.cer", "/wsc/wsc.cer", BindMode.READ_ONLY)
				.withClasspathResourceMapping("wsc/wsc.pem", "/wsc/wsc.pem", BindMode.READ_ONLY)

				.withClasspathResourceMapping("wsp/testserviceaa-ssl.cer", "/wsp/wspssl.cer", BindMode.READ_ONLY)
				.withClasspathResourceMapping("wsp/testserviceaa-ssl.pem", "/wsp/wspssl.pem", BindMode.READ_ONLY)

				.withClasspathResourceMapping("sts/sts.cer", "/trust/sts.cer", BindMode.READ_ONLY)
				
				.withClasspathResourceMapping("frontend/frontend.cer", "/trust/frontend.cer", BindMode.READ_ONLY)

				.withClasspathResourceMapping(config, "/configs/config.json", BindMode.READ_ONLY)
				.withCommand("--config", "/configs/config.json")
				.waitingFor(Wait.forLogMessage(".*serving initial configuration.*\\n", 1))
				.withNetworkAliases(alias);
		return kitCaddyContainer;						
	}

	private static File getKeycloakContainer(Network n) throws IOException, InterruptedException {
		GenericContainer<?> keycloackContainer = new GenericContainer<>("quay.io/keycloak/keycloak:23.0.7")
				.withClasspathResourceMapping("keycloak/test-realm.json", "/opt/keycloak/data/import/realm-test.json", BindMode.READ_ONLY)
				.withEnv("KEYCLOAK_ADMIN", "kit")
				.withEnv("KEYCLOAK_ADMIN_PASSWORD", "Test1234")
				.withEnv("KEYCLOAK_LOG_LEVEL", "DEBUG")
				//.withEnv("KEYCLOAK_IMPORT", "/importrealms/realm-test.json")
				.withCommand("start-dev --import-realm")
				.withNetwork(n)
				.withNetworkAliases("keycloak")
				.withExposedPorts(8080)
				.waitingFor(Wait.forHttp("/realms/test/protocol/saml/descriptor").withStartupTimeout(Duration.ofMinutes(3)));
		keycloackContainer.start();
		logContainerOutput(keycloackContainer, keycloakLogger);
		keycloakPort = keycloackContainer.getMappedPort(8080);
		keycloackHost = keycloackContainer.getContainerIpAddress();

		// Find the IDP certificate from keycloak and save it to temporary file for use in trust
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> idpMetadata = restTemplate.getForEntity("http://"+keycloackHost+":"+keycloakPort+"/realms/test/protocol/saml/descriptor", String.class);
		String metadata = idpMetadata.getBody();

		final String TAG_CERTIFICATE_START = "<ds:X509Certificate>";
		final String TAG_CERTIFICATE_END = "</ds:X509Certificate>";
		int startIndex = metadata.indexOf(TAG_CERTIFICATE_START);
		int endIndex = metadata.indexOf(TAG_CERTIFICATE_END);
		String certificateContent = metadata.substring(startIndex + TAG_CERTIFICATE_START.length(), endIndex);

		TemporaryFolder folder= new TemporaryFolder(); // It's a junit thing
		folder.create();
		File createdFile= folder.newFile("keycloak-idp.cer");
		BufferedWriter writer = new BufferedWriter(new FileWriter(createdFile));
		writer.append("-----BEGIN CERTIFICATE-----\n");
		writer.append(certificateContent);
		writer.append("-----END CERTIFICATE-----\n");
		writer.close();

		return createdFile;
	}


	public String addUserToKeycloak(String userName, String password/*, String pid, String cpr*/) throws JSONException  {

		// Auth
		String accessToken = getAccessToken();

		// User to create
		User user = new User();
		user.setUsername(userName);
		user.setEnabled(true);
		Credential credential = new Credential();
		credential.setType("password");
		credential.setValue(password);
		credential.setTemporary(false);
		user.getCredentials().add(credential);
		/*user.getAttributes().put("pid", pid);
		if (cpr != null) {
			user.getAttributes().put("cpr", cpr);
		}*/

		// Do create user
		HttpHeaders userheaders = getKeycloakApiHeaders(accessToken);
		HttpEntity<User> requestUser = new HttpEntity<User>(user, userheaders);
		ResponseEntity<String> result = restTemplate.postForEntity(appendToKeycloakHostAndPort("/admin/realms/test/users"), requestUser, String.class);
		if (HttpStatus.CREATED != result.getStatusCode()) {
			throw new RuntimeException("User not created");
		}

		return password;
	}

	public HttpHeaders getKeycloakApiHeaders(String accessToken) {
		HttpHeaders keycloakHeaders = new HttpHeaders();
		keycloakHeaders.setContentType(MediaType.APPLICATION_JSON);
		keycloakHeaders.set("Authorization", "bearer "+accessToken);
		return keycloakHeaders;
	}

	public String getAccessToken() throws JSONException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add("client_id", "admin-cli");
		map.add("username", "kit");
		map.add("password", "Test1234");
		map.add("grant_type", "password");
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(appendToKeycloakHostAndPort("/realms/master/protocol/openid-connect/token"), request, String.class);
		String authBody = response.getBody();
		JSONObject authJson = new JSONObject(authBody);
		String accessToken = authJson.getString("access_token");
		return accessToken;
	}

	public static String appendToKeycloakHostAndPort(String url) {
		return "http://"+keycloackHost+":"+keycloakPort+url;
	}

	protected static void logContainerOutput(GenericContainer<?> container, Logger logger) {
		logger.info("Attaching logger to container: " + container.getContainerInfo().getName());
		Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
		container.followOutput(logConsumer);
	}
	

	public static String getResourceAsString(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	    int nRead;
	    byte[] data = new byte[1024];
	    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
	        buffer.write(data, 0, nRead);
	    }
	 
	    buffer.flush();
	    byte[] byteArray = buffer.toByteArray();
	         
	    String text = new String(byteArray);
	    return text;
	}


}
