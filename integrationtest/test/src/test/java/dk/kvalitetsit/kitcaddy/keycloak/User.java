package dk.kvalitetsit.kitcaddy.keycloak;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class User {

	String username;
	
	Map<String, String> attributes = new HashMap<String, String>();
	
	List<Credential> credentials = new LinkedList<Credential>();
	
	boolean enabled;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public List<Credential> getCredentials() {
		return credentials;
	}

	public void setCredentials(List<Credential> credentials) {
		this.credentials = credentials;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
}