package org.cibseven.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor 
@JsonIgnoreProperties(ignoreUnknown = true)
public class StartForm {
	private String key;
	private String contextPath;
	private CamundaForm camundaFormRef;
}
