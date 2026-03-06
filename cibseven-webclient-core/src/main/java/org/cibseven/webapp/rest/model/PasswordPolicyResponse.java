package org.cibseven.webapp.rest.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor 
public class PasswordPolicyResponse {
    private boolean valid;
    private List<Map<String, Object>> rules;

}