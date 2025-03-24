package org.cibseven.webapp.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.cib.auth.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor  @JsonIgnoreProperties(ignoreUnknown = true)
public class CIBUser implements User {
	
	@Getter @Setter String authToken;
	@Getter @Setter protected String userID;
	@Setter String displayName;
	@Getter @Setter boolean isAnonUser;
	
	public CIBUser(String userId) {
		this.userID = userId;
	}

	@Override
	public String getId() {
		return userID;
	}

	@Override
	public String toString() {
		return userID; 
	}

	@Override
	public String getDisplayName() {
		if((displayName != null)&&(!displayName.isEmpty())) return displayName;
		else return userID;
	}

}