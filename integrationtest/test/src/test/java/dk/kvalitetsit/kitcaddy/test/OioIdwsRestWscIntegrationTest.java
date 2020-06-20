package dk.kvalitetsit.kitcaddy.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.GenericContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import dk.kvalitetsit.kitcaddy.AbstractIntegrationTest;
import dk.kvalitetsit.kitcaddy.TestConstants;

/**
 * 
 *    This testsetup
 * 
 *    | oio idws wsc |    ->    | oio idws wsp |     ->     | echo service |
 *
 */
public class OioIdwsRestWscIntegrationTest extends AbstractIntegrationTest {

	
	private static final String WSC_SERVICE_HOST = "wsc";
	private static final int WSC_SERVICE_PORT = 8686;
	private static final String WSC_SERVICE_URL = WSC_SERVICE_HOST+":"+WSC_SERVICE_PORT;

	private static final String WSP_SERVICE_HOST = "testserviceaa";
	private static final int WSP_SERVICE_PORT = 8443;
	private static final String WSP_SERVICE_URL = WSP_SERVICE_HOST+":"+WSP_SERVICE_PORT;

	@Rule
	public GenericContainer<?> wsc = createWsc();

	@Rule
	public GenericContainer<?> wsp = createWsp();

	public static GenericContainer<?> createWsc() {
		return 	getKitCaddyContainer(WSC_SERVICE_HOST, WSC_SERVICE_PORT, getDockerNetwork(), "wsc/wsc-functional.config");
	}

	public static GenericContainer<?> createWsp() {
		return 	getKitCaddyContainer(WSP_SERVICE_HOST, WSP_SERVICE_PORT, getDockerNetwork(), "wsp/wsp.config");
	}

	@Test
	public void testGetServiceResponseThroughWsc() throws JsonMappingException, JsonProcessingException {

		// Given
		String echoUrl = getWscServiceUrl()+"/echo";

		// When
		ResponseEntity<String> echoResponse = restTemplate.getForEntity(echoUrl, String.class);

		// Then
		ObjectMapper om = new ObjectMapper();
		Assert.assertNotNull(echoResponse);
		JsonNode responseParsed = om.readValue(echoResponse.getBody(), JsonNode.class);
		Assert.assertNotNull(responseParsed);

		JsonNode httpHeadersNode = responseParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY);
		Assert.assertNotNull(httpHeadersNode);
		JsonNode wspSessionDataHeaderNode = httpHeadersNode.get(TestConstants.WSP_SESSIONDATA_HEADERNAME);
		Assert.assertNotNull(wspSessionDataHeaderNode);
		
		String base64EncodedSessionData = wspSessionDataHeaderNode.asText();
		Assert.assertNotNull(base64EncodedSessionData);
		String decodedSessionData = new String(Base64.getDecoder().decode(base64EncodedSessionData));
		JsonNode sessionDataNode = om.readValue(decodedSessionData, JsonNode.class);
		Assert.assertNotNull(sessionDataNode);
	}

	@Test
	public void testGetServiceResponseThroughWscReusesSessionOnMultipleRequests() throws JsonMappingException, JsonProcessingException, URISyntaxException {

		// Given
		String echoUrl = getWscServiceUrl()+"/echo";
		String mySessionId = UUID.randomUUID().toString();
		HttpHeaders headers = new HttpHeaders();
		headers.add(TestConstants.SESSION_HEADER_NAME, mySessionId);
		RequestEntity<Void> requestEntity = new RequestEntity<Void>(headers, HttpMethod.GET, new URI(echoUrl));
		
		// When
		ResponseEntity<String> echoResponseFirst = restTemplate.exchange(requestEntity, String.class);
		ResponseEntity<String> echoResponseSecond = restTemplate.exchange(requestEntity, String.class);

		// Then
		ObjectMapper om = new ObjectMapper();
		Assert.assertNotNull(echoResponseFirst);
		JsonNode responseParsedFirst = om.readValue(echoResponseFirst.getBody(), JsonNode.class);
		Assert.assertNotNull(responseParsedFirst);

		Assert.assertNotNull(echoResponseSecond);
		JsonNode responseParsedSecond = om.readValue(echoResponseSecond.getBody(), JsonNode.class);
		Assert.assertNotNull(responseParsedSecond);

		JsonNode httpHeadersNodeFirst = responseParsedFirst.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY);
		Assert.assertNotNull(httpHeadersNodeFirst);

		JsonNode httpHeadersNodeSecond = responseParsedSecond.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY);
		Assert.assertNotNull(httpHeadersNodeSecond);

		TextNode wspSessionDataHeaderNodeFirst = (TextNode) httpHeadersNodeFirst.get(TestConstants.WSP_AUTHORIZATION_HEADERNAME);
		Assert.assertNotNull(wspSessionDataHeaderNodeFirst);

		TextNode wspSessionDataHeaderNodeSecond = (TextNode) httpHeadersNodeSecond.get(TestConstants.WSP_AUTHORIZATION_HEADERNAME);
		Assert.assertNotNull(wspSessionDataHeaderNodeSecond);

		Assert.assertEquals("Expected reuse of session on second request", wspSessionDataHeaderNodeFirst, wspSessionDataHeaderNodeSecond);
	}

	@Test
	public void testGetServiceResponseThroughWscWithClaims() throws JsonMappingException, JsonProcessingException, URISyntaxException {

		// Given
		String headerNameContentType = "Content-Type";
		ObjectMapper om = new ObjectMapper();
		String echoUrl = getWscServiceUrl()+"/echo";
		JsonNodeFactory nf = JsonNodeFactory.instance;
		ArrayNode claims = new ArrayNode(nf);
		ObjectNode claim = new ObjectNode(nf);
		String claimaValue = "I claim this";
		claim.set(TestConstants.WSC_XCLAIM_KEY, new TextNode(TestConstants.STS_ALLOWED_CLAIM_A));
		claim.set(TestConstants.WSC_XCLAIM_VALUE, new TextNode(claimaValue));
		claims.add(claim);
		String claimsHeaderValue = om.writeValueAsString(claims);
		String base64EncodedClaimsHeaderValue = Base64.getEncoder().encodeToString(claimsHeaderValue.getBytes());
		HttpHeaders headers = new HttpHeaders();
		headers.add(TestConstants.WSC_CLAIMS_HEADERNAME, base64EncodedClaimsHeaderValue);
		RequestEntity<Void> requestEntity = new RequestEntity<Void>(headers, HttpMethod.GET, new URI(echoUrl));

		// When
		ResponseEntity<String> echoResponse = restTemplate.exchange(requestEntity, String.class);

		// Then
		Assert.assertNotNull(echoResponse);
		JsonNode responseParsed = om.readValue(echoResponse.getBody(), JsonNode.class);
		Assert.assertNotNull(responseParsed);

		JsonNode httpHeadersNode = responseParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY);
		Assert.assertNotNull(httpHeadersNode);
		JsonNode wspSessionDataHeaderNode = httpHeadersNode.get(TestConstants.WSP_SESSIONDATA_HEADERNAME);
		Assert.assertNotNull(wspSessionDataHeaderNode);
		
		String base64EncodedSessionData = wspSessionDataHeaderNode.asText();
		Assert.assertNotNull(base64EncodedSessionData);
		String decodedSessionData = new String(Base64.getDecoder().decode(base64EncodedSessionData));
		JsonNode sessionDataNode = om.readValue(decodedSessionData, JsonNode.class);
		Assert.assertNotNull(sessionDataNode);
		JsonNode userAttributesNode = sessionDataNode.get(TestConstants.SESSION_DATA_USER_ATTRIBUTES_KEY);
		Assert.assertNotNull(userAttributesNode);
		JsonNode userAttributeClaimANode = userAttributesNode.get(TestConstants.STS_ALLOWED_CLAIM_A);
		Assert.assertNotNull(userAttributeClaimANode);
		Assert.assertTrue(userAttributeClaimANode.isArray());
		ArrayNode claimAs = (ArrayNode) userAttributeClaimANode;
		Assert.assertEquals(1, claimAs.size());
		Assert.assertTrue(userAttributeClaimANode.get(0).isValueNode());
		Assert.assertEquals(claimaValue, ((ValueNode)userAttributeClaimANode.get(0)).asText());
		HttpHeaders responseHeaders = echoResponse.getHeaders();
		Assert.assertNotNull(responseHeaders);
		List<String> responseContentTypes = responseHeaders.get(headerNameContentType);
		Assert.assertNotNull(responseContentTypes);
		Assert.assertEquals(1, responseContentTypes.size());
		Assert.assertEquals("application/json; charset=utf-8", responseContentTypes.get(0));
	}

	public String getWscServiceUrl() {
		return "http://"+wsc.getContainerIpAddress()+":"+wsc.getMappedPort(WSC_SERVICE_PORT);
	}
}
