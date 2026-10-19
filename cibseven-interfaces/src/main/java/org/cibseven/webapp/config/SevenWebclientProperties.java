/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.rest.RestTemplateConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Data;

/**
 * All settings of the CIB seven webclient, bound from {@code cibseven.webclient.*}. This class
 * is the single definition of what can be configured and of every default.
 *
 * <p>The field initializers are the defaults. They apply whenever a property is not
 * configured, both for the standalone application and when the webclient is embedded
 * into another Spring Boot application.</p>
 *
 * <p>A few settings are needed before beans exist, by {@code @ConditionalOnProperty},
 * {@code @RequestMapping} and {@code @Scheduled}. Those annotations reference the constants
 * below instead of spelling out property names, so names and defaults stay defined here.</p>
 */
@Data
@ConfigurationProperties(prefix = SevenWebclientProperties.PREFIX)
public class SevenWebclientProperties {

	public static final String PREFIX = "cibseven.webclient";

	/** Prefix of the modeler settings, for {@code @ConditionalOnProperty(name = "enabled")}; disabled by default. */
	public static final String MODELER_PREFIX = PREFIX + ".modeler";

	/** Prefix of the RestTemplate settings, for {@code @ConditionalOnProperty(name = "enabled", matchIfMissing = true)}. */
	public static final String REST_PREFIX = PREFIX + ".rest";

	/** Prefix of the plugin settings, for {@code @ConditionalOnProperty(name = "enabled")}; disabled by default. */
	public static final String PLUGINS_PREFIX = PREFIX + ".plugins";

	/** Placeholder of {@link Services#getBasePath()} for {@code @RequestMapping}. */
	public static final String SERVICES_BASE_PATH = "${" + PREFIX + ".services.basePath:" + Services.DEFAULT_BASE_PATH + "}";

	/** Placeholder of {@link Modeler.DeleteUsages#getCron()} for {@code @Scheduled}. */
	public static final String MODELER_DELETE_USAGES_CRON =
		"${" + MODELER_PREFIX + ".deleteUsages.cron:" + Modeler.DeleteUsages.DEFAULT_CRON + "}";

	/**
	 * Global enable/disable flag for the entire CIB seven webclient, read by
	 * {@code @ConditionalOnProperty(prefix = PREFIX, name = "enabled", matchIfMissing = true)}.
	 */
	private boolean enabled = true;

	/** Public URL of the webclient. */
	private String url = "";

	private Admin admin = new Admin();

	/**
	 * Legacy fallback used only with engine-rest versions prior to 2.2.0.
	 * Since 2.2.0 the history level is exposed by the engine configuration endpoint.
	 */
	private String historyLevel = "full";

	private String theme = "cib";

	private String productNamePageTitle = "CIB seven";

	/** Support dialog configuration, exposed to the frontend. */
	private String supportDialog = "";

	@NestedConfigurationProperty
	private EngineRestProperties engineRest = new EngineRestProperties();

	private Authentication authentication = new Authentication();

	private Sso sso = new Sso();

	private Ldap ldap = new Ldap();

	private Bpm bpm = new Bpm();

	private User user = new User();

	private Users users = new Users();

	@NestedConfigurationProperty
	private RestTemplateConfiguration rest = new RestTemplateConfiguration();

	private Services services = new Services();

	private Api api = new Api();

	private Link link = new Link();

	private Modeler modeler = new Modeler();

	private Persistence persistence = new Persistence();

	private Compatibility deprecated = new Compatibility();

	private Compatibility legacy = new Compatibility();

	private Plugins plugins = new Plugins();

	private StartProcess startProcess = new StartProcess();

	private Custom custom = new Custom();

	@Data
	public static class Admin {
		/** URL of the admin application. */
		private String url = "";
	}

	@Data
	public static class Authentication {
		/** Secret for signing the webclient tokens, base64 encoded and at least 155 characters long. */
		private String jwtSecret = "";
		private long tokenValidMinutes = 60;
		private long tokenProlongMinutes = 1440;
	}

	@Data
	public static class Sso {
		private boolean active = false;
		private Endpoints endpoints = new Endpoints();
		/** Required for the OAuth2 and ADFS user providers. */
		private String clientId;
		private String clientSecret;
		private String scopes = "";
		private String domain = "";
		/** Required for the OAuth2 and ADFS user providers. */
		private String userIdProperty;
		/** Required for the OAuth2 and ADFS user providers. */
		private String userNameProperty;
		/** Required for the ADFS user provider. */
		private String technicalUserId;
		private boolean infoInIdToken = false;
		/** Forward the SSO access token to the engine REST API. */
		private boolean accessTokenToEngineRest = false;
		private Assertion assertion = new Assertion();

		@Data
		public static class Endpoints {
			private String authorization = "";
			/** Required for the OAuth2 and ADFS user providers. */
			private String token;
			/** Required for the OAuth2 and ADFS user providers. */
			private String jwks;
			/** Required for the OAuth2 user provider. */
			private String user;
			private String introspection = "";
			/** RFC 7009 token revocation endpoint. When blank, tokens are not revoked on logout. */
			private String revocation = "";
			/**
			 * OIDC RP-Initiated Logout end_session_endpoint. When blank, the identity provider session is
			 * left untouched. Register {@code <app url>/} as a valid post logout redirect URI at the IdP.
			 */
			private String endSession = "";
		}

		@Data
		public static class Assertion {
			/** Client assertion type (JJWT, AZURE_WORKLOAD_IDENTITY), used instead of a client secret. */
			private String type = "";
			/** Location of the PEM-encoded PKCS#8 private key file, supports classpath: and file:. */
			private String keyLocation = "";
		}
	}

	@Data
	public static class Ldap {
		private String url = "ldap://ldap.cibseven.org";
		private String user = "";
		private String password = "";
		private String folder = "DC=cibseven,DC=org";
		private String userClass = "person";
		private String userNameAttribute = "samAccountName";
		private String userDisplayNameAttribute = "name";
		private Attributes attributes = new Attributes();
		private String modifiedDateFormat = "yyyyMMddHHmmss.SX";
		private int countLimit = 400;
		private String followReferrals = "follow";

		@Data
		public static class Attributes {
			/** Attributes searched by the user search, separated by semicolons. */
			private String filters = "samAccountName;name";
		}
	}

	@Data
	public static class Bpm {
		private String provider = "org.cibseven.webapp.providers.SevenProvider";
	}

	@Data
	public static class User {
		/**
		 * User provider. Alternatives: org.cibseven.webapp.auth.LdapUserProvider,
		 * org.cibseven.webapp.auth.OAuth2UserProvider, org.cibseven.webapp.auth.AdfsUserProvider
		 */
		private String provider = "org.cibseven.webapp.auth.SevenUserProvider";
		/** Whether users can be edited, determined by the frontend if not set. */
		private Boolean editable;
		private Boolean userPasswordChangeEnabled = true;
	}

	@Data
	public static class Users {
		private Search search = new Search();

		@Data
		public static class Search {
			/** Wildcard used for the users search, "*" if empty. */
			private String wildcard = "";
		}
	}

	@Data
	public static class Services {
		static final String DEFAULT_BASE_PATH = "services/v1";

		/** Base path of the webclient REST services, read by {@code @RequestMapping(SERVICES_BASE_PATH)}. */
		private String basePath = DEFAULT_BASE_PATH;
	}

	@Data
	public static class Api {
		private Common common = new Common();

		@Data
		public static class Common {
			private String title = "CIB seven webclient API";
			private String description = "This API exposes the functionality of CIB seven webclient as a REST service under the Apache License 2.0.";
			private String termsOfService = "https://www.apache.org/licenses/LICENSE-2.0";
			private String license = "Apache 2.0";
			private String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0";
			private Contact contact = new Contact();
			/** Webclient server url for the swagger-ui, only needed if the automatic detection is wrong. */
			private String url = "";

			@Data
			public static class Contact {
				private String name = "CIB seven";
				private String url = "https://cibseven.org";
				private String email = "info@cibseven.org";
			}
		}
	}

	@Data
	public static class Link {
		private String terms = "";
		private String privacy = "";
		private String imprint = "";
		private String accessibility = "";
		private String help = "";
	}

	@Data
	public static class Modeler {
		/**
		 * Enable or disable the modeler (JPA entities, repositories, REST endpoints).
		 * Requires a configured DataSource and pre-created schema. Read by
		 * {@code @ConditionalOnProperty(prefix = MODELER_PREFIX, name = "enabled")}.
		 */
		private boolean enabled = false;
		private Session session = new Session();
		private DeleteUsages deleteUsages = new DeleteUsages();
		private Templates templates = new Templates();

		@Data
		public static class Session {
			private int expiresAfterMin = 10;
		}

		@Data
		public static class DeleteUsages {
			static final String DEFAULT_CRON = " 0 0 0 * * ?";

			private int usageLimit = 50;
			/** Cleanup schedule of old modeler sessions, read by {@code @Scheduled(cron = MODELER_DELETE_USAGES_CRON)}. */
			private String cron = DEFAULT_CRON;
		}

		@Data
		public static class Templates {
			/**
			 * Element template JSONs loaded into the modeler database at startup, as Spring resource
			 * locations (globs resolved), e.g. {@code file:/opt/cibseven/element-templates/*.json}.
			 */
			private List<String> paths = new ArrayList<>();
			/**
			 * Attempts to verify that the {@code MOD_ELEMENT_TEMPLATES} table exists before loading.
			 * The table is created by the process engine (MyBatis), which cannot be ordered against
			 * the webclient deployment, so on external databases and slow environments it may not
			 * exist yet when the loader runs.
			 */
			private int schemaReadyMaxAttempts = 30;
			/** Delay between schema-readiness attempts in milliseconds, 60s in total by default. */
			private long schemaReadyRetryDelayMs = 2000;
		}
	}

	@Data
	public static class Persistence {
		/**
		 * Additional Hibernate properties of the webclient's own persistence unit, the one holding
		 * the modeler tables, e.g. {@code hibernate.default_schema}. They apply to this unit only,
		 * unlike {@code spring.jpa.*}, and override {@link #DEFAULTS}.
		 */
		private Map<String, String> properties = new LinkedHashMap<>();

		/**
		 * Defaults of the webclient's unit, independent of the application's global {@code spring.jpa.*}
		 * settings: table names are used exactly as written in the entities, and the schema is not
		 * generated, because it is created by the database scripts of the process engine.
		 */
		public static final Map<String, String> DEFAULTS = Map.of(
			"hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
			"hibernate.hbm2ddl.auto", "none");

		/** @return {@link #DEFAULTS} overridden by the configured {@link #properties} */
		public Map<String, String> effectiveProperties() {
			Map<String, String> effective = new LinkedHashMap<>(DEFAULTS);
			effective.putAll(properties);
			return effective;
		}
	}

	@Data
	public static class Plugins {
		/** Load frontend plugins from the classpath below META-INF/cibseven-plugins/, one folder per plugin. Plugin code runs inside the webclient page. */
		private boolean enabled = false;
		/** Ids of plugins to skip, e.g. one that breaks the page; takes a restart. */
		private List<String> disabled = new ArrayList<>();
	}

	@Data
	public static class StartProcess {
		/**
		 * Show a default start form (business key and manual variable creation) when the process
		 * definition has no start form. Disabled to keep the "instant start" behavior.
		 */
		private boolean defaultFormEnabled = false;
	}

	@Data
	public static class Compatibility {
		/** Check permissions for some use cases in the webclient backend (behaviour until 2.2.0). */
		private Toggle authorization = new Toggle(false);
	}

	@Data
	public static class Toggle {
		private boolean enabled;

		public Toggle() {
		}

		public Toggle(boolean enabled) {
			this.enabled = enabled;
		}
	}

	@Data
	public static class Custom {
		private Spring spring = new Spring();

		@Data
		public static class Spring {
			private Jackson jackson = new Jackson();

			@Data
			public static class Jackson {
				private Parser parser = new Parser();
				private Serialization serialization = new Serialization();

				@Data
				public static class Parser {
					/** Maximum length of a JSON string value. */
					private int maxSize = 20_000_000;
				}

				@Data
				public static class Serialization {
					/**
					 * Deprecated interim opt-out, removed with the Jackson 3 migration: serializes
					 * java.util.Date as epoch millis and java.time types as numeric arrays again.
					 * Prefer adapting the consumer or annotating the field with @JsonFormat.
					 */
					private boolean writeDatesAsTimestamps = false;
				}
			}
		}
	}
}
