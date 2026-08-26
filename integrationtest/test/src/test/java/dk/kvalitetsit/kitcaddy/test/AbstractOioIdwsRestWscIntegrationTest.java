package dk.kvalitetsit.kitcaddy.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.*;

import dk.kvalitetsit.kitcaddy.AbstractIntegrationTest;
import dk.kvalitetsit.kitcaddy.TestConstants;

public abstract class AbstractOioIdwsRestWscIntegrationTest extends AbstractIntegrationTest {

	
	@Test
	public void testGetServiceResponseThroughWsc() throws JacksonException {

		// Given
		String echoUrl = getWscServiceUrl()+"/echo";

		// When
		ResponseEntity<String> echoResponse = restTemplate.getForEntity(echoUrl, String.class);

		// Then
		ObjectMapper om = new ObjectMapper();
		assertNotNull(echoResponse);
		JsonNode responseParsed = om.readValue(echoResponse.getBody(), JsonNode.class);
		assertNotNull(responseParsed);

		JsonNode httpHeadersNode = responseParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY);
		assertNotNull(httpHeadersNode);
		JsonNode wspSessionDataHeaderNode = httpHeadersNode.get(TestConstants.WSP_SESSIONDATA_HEADERNAME);
		assertNotNull(wspSessionDataHeaderNode);
		
		String base64EncodedSessionData = wspSessionDataHeaderNode.asString();
		assertNotNull(base64EncodedSessionData);
		String decodedSessionData = new String(Base64.getDecoder().decode(base64EncodedSessionData));
		JsonNode sessionDataNode = om.readValue(decodedSessionData, JsonNode.class);
		assertNotNull(sessionDataNode);
	}

	@Test
	public void testGetServiceResponseThroughWscReusesSessionOnMultipleRequests() throws JacksonException, URISyntaxException {

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
		assertNotNull(echoResponseFirst);
		JsonNode responseParsedFirst = om.readValue(echoResponseFirst.getBody(), JsonNode.class);
		assertNotNull(responseParsedFirst);

		assertNotNull(echoResponseSecond);
		JsonNode responseParsedSecond = om.readValue(echoResponseSecond.getBody(), JsonNode.class);
		assertNotNull(responseParsedSecond);

		JsonNode httpHeadersNodeFirst = responseParsedFirst.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY);
		assertNotNull(httpHeadersNodeFirst);

		JsonNode httpHeadersNodeSecond = responseParsedSecond.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY);
		assertNotNull(httpHeadersNodeSecond);

		JsonNode wspSessionDataHeaderNodeFirst = httpHeadersNodeFirst.get(TestConstants.WSP_AUTHORIZATION_HEADERNAME);
		assertNotNull(wspSessionDataHeaderNodeFirst);

		JsonNode wspSessionDataHeaderNodeSecond = httpHeadersNodeSecond.get(TestConstants.WSP_AUTHORIZATION_HEADERNAME);
		assertNotNull(wspSessionDataHeaderNodeSecond);

		assertEquals(wspSessionDataHeaderNodeFirst, wspSessionDataHeaderNodeSecond, "Expected reuse of session on second request");
	}

	@Test
	public void testGetServiceResponseThroughWscWithClaims() throws JacksonException, URISyntaxException {

		// Given
		String headerNameContentType = "Content-Type";
		ObjectMapper om = new ObjectMapper();
		String echoUrl = getWscServiceUrl()+"/echo";
		JsonNodeFactory nf = JsonNodeFactory.instance;
		ArrayNode claims = new ArrayNode(nf);
		ObjectNode claim = new ObjectNode(nf);
		String claimaValue = "I claim this";
		claim.set(TestConstants.WSC_XCLAIM_KEY, new StringNode(TestConstants.STS_ALLOWED_CLAIM_A));
		claim.set(TestConstants.WSC_XCLAIM_VALUE, new StringNode(claimaValue));
		claims.add(claim);
		String claimsHeaderValue = om.writeValueAsString(claims);
		String base64EncodedClaimsHeaderValue = Base64.getEncoder().encodeToString(claimsHeaderValue.getBytes());
		HttpHeaders headers = new HttpHeaders();
		headers.add(TestConstants.WSC_CLAIMS_HEADERNAME, base64EncodedClaimsHeaderValue);
		RequestEntity<Void> requestEntity = new RequestEntity<Void>(headers, HttpMethod.GET, new URI(echoUrl));

		// When
		ResponseEntity<String> echoResponse = restTemplate.exchange(requestEntity, String.class);

		// Then
		assertNotNull(echoResponse);
		JsonNode responseParsed = om.readValue(echoResponse.getBody(), JsonNode.class);
		assertNotNull(responseParsed);

		JsonNode httpHeadersNode = responseParsed.get(TestConstants.ECHO_SERVICE_HTTP_HEADER_KEY);
		assertNotNull(httpHeadersNode);
		JsonNode wspSessionDataHeaderNode = httpHeadersNode.get(TestConstants.WSP_SESSIONDATA_HEADERNAME);
		assertNotNull(wspSessionDataHeaderNode);
		
		String base64EncodedSessionData = wspSessionDataHeaderNode.asString();
		assertNotNull(base64EncodedSessionData);
		String decodedSessionData = new String(Base64.getDecoder().decode(base64EncodedSessionData));
		JsonNode sessionDataNode = om.readValue(decodedSessionData, JsonNode.class);
		assertNotNull(sessionDataNode);
		JsonNode userAttributesNode = sessionDataNode.get(TestConstants.SESSION_DATA_USER_ATTRIBUTES_KEY);
		assertNotNull(userAttributesNode);
		JsonNode userAttributeClaimANode = userAttributesNode.get(TestConstants.STS_ALLOWED_CLAIM_A);
		assertNotNull(userAttributeClaimANode);
		assertTrue(userAttributeClaimANode.isArray());
		ArrayNode claimAs = (ArrayNode) userAttributeClaimANode;
		assertEquals(1, claimAs.size());
		assertTrue(userAttributeClaimANode.get(0).isValueNode());
		assertEquals(claimaValue, ((ValueNode)userAttributeClaimANode.get(0)).asString());
		HttpHeaders responseHeaders = echoResponse.getHeaders();
		assertNotNull(responseHeaders);
		List<String> responseContentTypes = responseHeaders.get(headerNameContentType);
		assertNotNull(responseContentTypes);
		assertEquals(1, responseContentTypes.size());
		assertEquals("application/json; charset=utf-8", responseContentTypes.get(0));
	}

	public abstract String getWscServiceUrl();
}
