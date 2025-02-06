package org.cibseven.auth;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class SevenAdminAuth {
	private String userId;
	private List<String> authorizedApps;
}
