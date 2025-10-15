package org.cibseven.webapp.rest.model;

public class InfoVersion {
	
	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

}
