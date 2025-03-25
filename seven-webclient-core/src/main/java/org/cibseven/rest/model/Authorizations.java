package org.cibseven.rest.model;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class Authorizations {
	private Collection<Authorization> application;
	private Collection<Authorization> processDefinition;
	private Collection<Authorization> processInstance;
	private Collection<Authorization> task;
	private Collection<Authorization> filter;
	private Collection<Authorization> authorization;
	private Collection<Authorization> user;
	private Collection<Authorization> group;
}
