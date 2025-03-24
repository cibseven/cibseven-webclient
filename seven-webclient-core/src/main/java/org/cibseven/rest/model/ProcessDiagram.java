package org.cibseven.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessDiagram {

	String id;
	String bpmn20Xml;
	
}
