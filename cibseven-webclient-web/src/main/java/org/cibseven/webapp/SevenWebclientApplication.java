package org.cibseven.webapp;

import org.glassfish.jersey.server.ResourceConfig;


public class SevenWebclientApplication extends ResourceConfig {
	
	public SevenWebclientApplication() {
		System.out.println("Initializing SevenWebclientApplication");
		packages("org.cibseven.webapp.rest");
	}
}