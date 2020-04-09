package dk.kvalitetsit.kitcaddy.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.kvalitetsit.kitcaddy.AbstractIntegrationTest;
import dk.kvalitetsit.kitcaddy.TestConstants;

/**
 * Tests the configuration file generator - ie the individual templates
 *
 */
public class TemplateIntegrationTest extends AbstractIntegrationTest {

	private GenericContainer<?> templateContainer; 
	
	@Before
	public void setupTest() throws IOException {
		templateContainer = new GenericContainer<>("kvalitetsit/kitcaddy-templates:dev")
				.withEnv("CADDYFILE", "/configs/myoutput")
				.withNetwork(getDockerNetwork())
				.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
	}
	
	
	@Test
	public void testSamlServiceTemplate() throws IOException {

		// Given
		ObjectMapper om = new ObjectMapper();
		String templateToTest = "/caddyfiletemplates/samlprovider/samlprovider.json";
		templateContainer.withEnv("TEMPLATE_FILE", templateToTest);
		templateContainer.withClasspathResourceMapping("/testdata/sp-fragment.json", "/input/extra.json", BindMode.READ_ONLY);
		templateContainer.withEnv("CADDYFILE_APPEND_TO", "/input/extra.json");
		populateEnviromentSamlService(templateContainer);
		JsonNode jsonConfigToCompareWith = om.readValue(TemplateIntegrationTest.class.getResourceAsStream("/samlserviceprovider/saml.config"), JsonNode.class);
		
		// When
		templateContainer.start();
		
		// Then
		String output = templateContainer.getLogs(OutputType.STDOUT);
		Assert.assertNotNull("Expected output from template container", output);
		JsonNode jsonConfig = om.readValue(output, JsonNode.class);
		Assert.assertNotNull(jsonConfig);
		Assert.assertEquals(jsonConfigToCompareWith, jsonConfig);
	}

	private void populateEnviromentSamlService(GenericContainer<?> templateContainer) {
		templateContainer.withEnv("SAML_CLIENT_LOGLEVEL", "debug");
		templateContainer.withEnv("LISTEN_PORT", "8787");
		templateContainer.withEnv("METRICS_PATH", "/metrics");
		templateContainer.withEnv("SAML_SESSION_HEADER", TestConstants.SESSION_HEADER_NAME);
		templateContainer.withEnv("SAML_SESSION_EXPIRY_HOURS", "6");
		templateContainer.withEnv("MONGO_HOST", "mongo");
		templateContainer.withEnv("MONGO_DATABASE", "samlsp");
		templateContainer.withEnv("SAML_AUDIENCE_RESTRICTION", "test");
		templateContainer.withEnv("SAML_IDP_METADATAURL", "http://keycloak:8080/auth/realms/test/protocol/saml/descriptor");
		templateContainer.withEnv("SAML_ENTITY_ID", "test");
		templateContainer.withEnv("SAML_SIGN_AUTH_REQUEST", "false");
		templateContainer.withEnv("SAML_SIGN_CERT_FILE", "/sp/sp.cer");
		templateContainer.withEnv("SAML_SIGN_KEY_FILE", "/sp/sp.pem");
		templateContainer.withEnv("SAML_EXTERNAL_URL", "http://uiservice:8787");
		templateContainer.withEnv("SAML_METADATA_PATH", "/saml/metadata");
		templateContainer.withEnv("SAML_LOGOUT_PATH", "/saml/logout");
		templateContainer.withEnv("SAML_SLO_PATH", "/saml/SLO");
		templateContainer.withEnv("SAML_SSO_PATH", "/saml/SSO");
		templateContainer.withEnv("SAML_COOKIE_DOMAIN", "");
		templateContainer.withEnv("SAML_COOKIE_PATH", "/");
		templateContainer.withEnv("SAML_BACKEND_HOST", "localhost");
		templateContainer.withEnv("SAML_BACKEND_PORT", "9090");
	}
}
