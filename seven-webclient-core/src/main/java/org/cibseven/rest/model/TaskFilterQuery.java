package org.cibseven.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskFilterQuery {
	String nameLike;
	String assigneeLike;
	String processDefinitionId;
	String processInstanceBusinessKeyLike;
	List<ProcessVariablesCriteria> processVariables;
}
