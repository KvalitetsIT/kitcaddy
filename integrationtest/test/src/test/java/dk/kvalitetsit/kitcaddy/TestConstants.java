package dk.kvalitetsit.kitcaddy;

public class TestConstants {

	public final static String ECHO_SERVICE_HTTP_HEADER_KEY = "headers";

	public static final String WSC_XCLAIM_KEY = "key";
	public static final String WSC_XCLAIM_VALUE = "value";

	
	public static final String SP_MONGO_DATABASE = "samlsp";
	public static final String SP_MONGO_SESSION_COLLECTION = "samlsessions";
	public static final String SP_MONGO_SESSION_ID_COLUMN = "sessionid";
	
	
	public static final String WSC_MONGO_DATABASE = "wsctest";
	public static final String WSC_MONGO_SESSION_COLLECTION = "wscsessions";
	public static final String WSC_CLAIMS_HEADERNAME = "X-Claims";

	
	public final static String WSP_SESSIONDATA_HEADERNAME = "sessiondataheader";
	public final static String WSP_AUTHORIZATION_HEADERNAME = "authorization";

	
	public final static String SESSION_DATA_USER_ATTRIBUTES_KEY = "UserAttributes";
	
	
	
	public final static String SESSION_HEADER_NAME = "session";

	public static final String STS_ALLOWED_CLAIM_A = "claim-a";
	public static final String STS_ALLOWED_CLAIM_B = "claim-b";


	public static final String KEYCLOAK_ACCOUNT_URL = "http://keycloak:8080/auth/realms/test/account";
}
