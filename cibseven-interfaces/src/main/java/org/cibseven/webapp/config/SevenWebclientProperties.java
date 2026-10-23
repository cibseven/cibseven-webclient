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
 * into another Spring Boot application. A setting without a default has to be configured
 * when the feature that needs it is used.</p>
 *
 * <p>The Javadoc of each field is its documentation: the build turns it into
 * {@code META-INF/spring-configuration-metadata.json}, which IDEs use for completion and from
 * which the list of configuration options can be generated.</p>
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

	/** Whether the webclient is active at all. When false, none of its beans and REST endpoints are created. */
	private boolean enabled = true;

	/**
	 * URL of a webclient whose /auth endpoint the generic user provider asks for the
	 * user's token. Not asked when empty.
	 */
	private String url = "";

	/** Settings of the admin application used by the generic user provider. */
	private Admin admin = new Admin();

	/**
	 * History level reported to the frontend when the engine does not expose its configuration,
	 * i.e. engine-rest versions before 2.2.0 (audit, full, activity, none). Newer engines report
	 * their own history level.
	 */
	private String historyLevel = "full";

	/** Bootstrap-based theme of the frontend. Only "cib" is supported. */
	private String theme = "cib";

	/** Product name shown in the browser page title. */
	private String productNamePageTitle = "CIB seven";

	/** JSON configuration of the support dialog in the frontend. Not shown when empty or invalid. */
	private String supportDialog = "";

	/** Engine REST API the webclient connects to, and optional additional engines. */
	@NestedConfigurationProperty
	private EngineRestProperties engineRest = new EngineRestProperties();

	/** Tokens the webclient issues to logged in users. */
	private Authentication authentication = new Authentication();

	/** Single sign-on with an OAuth2/OpenID Connect provider or ADFS. */
	private Sso sso = new Sso();

	/** LDAP server used by the LDAP user provider. */
	private Ldap ldap = new Ldap();

	/** Connection to the process engine. */
	private Bpm bpm = new Bpm();

	/** Authentication and user management. */
	private User user = new User();

	/** User search. */
	private Users users = new Users();

	/** HTTP client used for the calls to the engine REST API. */
	@NestedConfigurationProperty
	private RestTemplateConfiguration rest = new RestTemplateConfiguration();

	/** REST services of the webclient backend. */
	private Services services = new Services();

	/** OpenAPI documentation of the webclient REST services. */
	private Api api = new Api();

	/** Links shown in the frontend. Not shown when empty. */
	private Link link = new Link();

	/** Process and form modeler. */
	private Modeler modeler = new Modeler();

	/** JPA persistence unit holding the webclient's own tables. */
	private Persistence persistence = new Persistence();

	/** Behavior that is deprecated and will be removed. */
	private DeprecatedFeatures deprecated = new DeprecatedFeatures();

	/** Compatibility with engines before 2.2.0. */
	private LegacyFeatures legacy = new LegacyFeatures();

	/** Frontend plugins. */
	private Plugins plugins = new Plugins();

	/** Starting process instances. */
	private StartProcess startProcess = new StartProcess();

	/** Settings for the frameworks the webclient uses. */
	private Custom custom = new Custom();

	@Data
	public static class Admin {
		/** URL of the admin application that the generic user provider reads the user from. */
		private String url = "";
	}

	@Data
	public static class Authentication {
		/**
		 * Secret for signing the webclient tokens. Base64 encoded and at least 155 characters long;
		 * the webclient does not start without it.
		 */
		private String jwtSecret = "";
		/** Minutes a token is valid. */
		private long tokenValidMinutes = 60;
		/** Minutes after its expiry within which a token can still be prolonged without a new login. */
		private long tokenProlongMinutes = 1440;
	}

	@Data
	public static class Sso {
		/** Whether the frontend redirects to the identity provider for the login. */
		private boolean active = false;
		/** Endpoints of the identity provider. */
		private Endpoints endpoints = new Endpoints();
		/** Client id of the webclient at the identity provider. Required for SSO. */
		private String clientId;
		/** Client secret of the webclient. Required for ADFS; for OAuth2 unless an assertion is configured. */
		private String clientSecret;
		/** Scopes requested in the authorization request, separated by spaces. */
		private String scopes = "";
		/** ADFS: domain appended to user names given without one at the password login. */
		private String domain = "";
		/** Claim of the token holding the user id. Required for SSO. */
		private String userIdProperty;
		/** Claim of the token holding the user's display name. Required for SSO. */
		private String userNameProperty;
		/** ADFS: user id assigned to tokens the identity provider issued to applications. Required for ADFS. */
		private String technicalUserId;
		/** ADFS: read the user claims from the id token instead of the access token. */
		private boolean infoInIdToken = false;
		/**
		 * Forward the identity provider's access token to the engine REST API instead of a webclient
		 * token, needed when the engine REST API is protected by Spring Security.
		 */
		private boolean accessTokenToEngineRest = false;
		/** Client assertion used instead of a client secret. */
		private Assertion assertion = new Assertion();

		@Data
		public static class Endpoints {
			/** Authorization endpoint the frontend redirects to for the login. */
			private String authorization = "";
			/** Token endpoint. Required for SSO. */
			private String token;
			/** JWKS endpoint with the keys that sign the tokens. Required for SSO. */
			private String jwks;
			/** OAuth2: user info endpoint. Required for OAuth2. */
			private String user;
			/** OAuth2: token introspection endpoint, used for access tokens that are no JWTs. */
			private String introspection = "";
			/** RFC 7009 token revocation endpoint. When empty, tokens are not revoked on logout. */
			private String revocation = "";
			/**
			 * OIDC RP-Initiated Logout end_session_endpoint. When empty, the identity provider session is
			 * left untouched. Register "<app url>/" as a valid post logout redirect URI at the IdP.
			 */
			private String endSession = "";
		}

		@Data
		public static class Assertion {
			/** Client assertion type used instead of a client secret: JJWT or AZURE_WORKLOAD_IDENTITY. */
			private String type = "";
			/**
			 * Location of the PEM-encoded PKCS#8 private key, with classpath: or file: prefix.
			 * Required for JJWT, not used for AZURE_WORKLOAD_IDENTITY.
			 */
			private String keyLocation = "";
		}
	}

	@Data
	public static class Ldap {
		/** URL of the LDAP server, e.g. ldap://ldap.example.com. Required for LDAP. */
		private String url;
		/** User the webclient binds with to search for users. */
		private String user = "";
		/** Password of the bind user. */
		private String password = "";
		/** Base DN of the user search, e.g. DC=example,DC=com. Required for LDAP. */
		private String folder;
		/** Object class of user entries. */
		private String userClass = "person";
		/** Attribute holding the user name. */
		private String userNameAttribute = "samAccountName";
		/** Attribute holding the user's display name. */
		private String userDisplayNameAttribute = "name";
		/** SimpleDateFormat pattern of the modifyTimestamp attribute. */
		private String modifiedDateFormat = "yyyyMMddHHmmss.SX";
		/** Maximum number of entries a search returns. */
		private int countLimit = 400;
		/** How referrals are handled: follow, ignore or throw. */
		private String followReferrals = "follow";
	}

	@Data
	public static class Bpm {
		/**
		 * Class connecting the webclient to the engine: org.cibseven.webapp.providers.SevenProvider
		 * calls the engine REST API, org.cibseven.webapp.providers.SevenDirectProvider calls an engine
		 * running in the same application.
		 */
		private String provider = "org.cibseven.webapp.providers.SevenProvider";
	}

	@Data
	public static class User {
		/**
		 * Class authenticating the users: org.cibseven.webapp.auth.SevenUserProvider (engine users),
		 * LdapUserProvider, OAuth2UserProvider or AdfsUserProvider from the same package.
		 */
		private String provider = "org.cibseven.webapp.auth.SevenUserProvider";
		/** Whether users can be created and edited in the frontend. When not set, only with the SevenUserProvider. */
		private Boolean editable;
		/** Whether users can change their password in the frontend. */
		private Boolean userPasswordChangeEnabled = true;
	}

	@Data
	public static class Users {
		/** User search. */
		private Search search = new Search();

		@Data
		public static class Search {
			/** Wildcard that replaces "*" in user search terms, e.g. "%". "*" is kept when empty. */
			private String wildcard = "";
		}
	}

	@Data
	public static class Services {
		static final String DEFAULT_BASE_PATH = "services/v1";

		/** Path of the webclient REST services, relative to the application path. */
		private String basePath = DEFAULT_BASE_PATH;
	}

	@Data
	public static class Api {
		/** Information shown in the OpenAPI documentation and the Swagger UI. */
		private Common common = new Common();

		@Data
		public static class Common {
			/** API title. */
			private String title = "CIB seven webclient API";
			/** API description. */
			private String description = "This API exposes the functionality of CIB seven webclient as a REST service under the Apache License 2.0.";
			/** URL of the terms of service. */
			private String termsOfService = "https://www.apache.org/licenses/LICENSE-2.0";
			/** License name. */
			private String license = "Apache 2.0";
			/** URL of the license. */
			private String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0";
			/** Contact. */
			private Contact contact = new Contact();
			/** Server URL used by the Swagger UI. Only needed when the automatic detection is wrong. */
			private String url = "";

			@Data
			public static class Contact {
				/** Contact name. */
				private String name = "CIB seven";
				/** Contact URL. */
				private String url = "https://cibseven.org";
				/** Contact email. */
				private String email = "info@cibseven.org";
			}
		}
	}

	@Data
	public static class Link {
		/** URL of the terms of use. */
		private String terms = "";
		/** URL of the privacy policy. */
		private String privacy = "";
		/** URL of the imprint. */
		private String imprint = "";
		/** URL of the accessibility statement. */
		private String accessibility = "";
		/** URL of the help. */
		private String help = "";
	}

	@Data
	public static class Modeler {
		/**
		 * Whether the modeler (JPA entities, repositories, REST endpoints) is active. Requires a
		 * data source and the modeler tables, which are created by the database scripts of the engine.
		 */
		private boolean enabled = false;
		/** Editing sessions of diagrams and forms. */
		private Session session = new Session();
		/** Cleanup of old editing sessions. */
		private DeleteUsages deleteUsages = new DeleteUsages();
		/** Element templates loaded at startup. */
		private Templates templates = new Templates();

		@Data
		public static class Session {
			/** Minutes after which an editing session counts as closed and the diagram or form is free again. */
			private int expiresAfterMin = 10;
		}

		@Data
		public static class DeleteUsages {
			static final String DEFAULT_CRON = " 0 0 0 * * ?";

			/** Number of most recent editing sessions kept by the cleanup. */
			private int usageLimit = 50;
			/** Cron expression of the cleanup. */
			private String cron = DEFAULT_CRON;
		}

		@Data
		public static class Templates {
			/**
			 * Element template JSONs loaded into the modeler database at startup, as Spring resource
			 * locations (globs resolved), e.g. file:/opt/cibseven/element-templates/*.json.
			 */
			private List<String> paths = new ArrayList<>();
			/**
			 * Attempts to verify that the MOD_ELEMENT_TEMPLATES table exists before loading.
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
		 * Additional Hibernate properties of the webclient's own persistence unit, the one holding the
		 * modeler tables, e.g. hibernate.default_schema. They apply to this unit only, unlike
		 * spring.jpa.*, and override its defaults: hibernate.physical_naming_strategy=
		 * org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl, so that table names are
		 * used exactly as written in the entities, and hibernate.hbm2ddl.auto=none, because the schema
		 * is created by the database scripts of the process engine.
		 */
		private Map<String, String> properties = new LinkedHashMap<>();

		/**
		 * @return the defaults described at {@link #properties}, independent of the application's
		 *         global spring.jpa.* settings, overridden by the configured {@link #properties}
		 */
		public Map<String, String> effectiveProperties() {
			Map<String, String> effective = new LinkedHashMap<>(Map.of(
				"hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
				"hibernate.hbm2ddl.auto", "none"));
			effective.putAll(properties);
			return effective;
		}
	}

	@Data
	public static class Plugins {
		/**
		 * Whether frontend plugins are loaded from the classpath below META-INF/cibseven-plugins/,
		 * one folder per plugin. Plugin code runs inside the webclient page.
		 */
		private boolean enabled = false;
		/** Ids of plugins to skip, e.g. one that breaks the page. Takes a restart. */
		private List<String> disabled = new ArrayList<>();
	}

	@Data
	public static class StartProcess {
		/**
		 * Show a default start form (business key and manual variable creation) when the process
		 * definition has no start form. Disabled by default to keep the instant start.
		 */
		private boolean defaultFormEnabled = false;
	}

	@Data
	public static class DeprecatedFeatures {
		/** Permission checks in the webclient backend. */
		private BackendAuthorization authorization = new BackendAuthorization();

		@Data
		public static class BackendAuthorization {
			/**
			 * Check permissions for some use cases in the webclient backend as well, the behavior
			 * until 2.2.0. Since then the engine checks them. Will be removed.
			 */
			private boolean enabled = false;
		}
	}

	@Data
	public static class LegacyFeatures {
		/** Authorization reported to the frontend. */
		private FrontendAuthorization authorization = new FrontendAuthorization();

		@Data
		public static class FrontendAuthorization {
			/**
			 * Report authorization as enabled to the frontend even when the engine configuration says
			 * it is disabled.
			 */
			private boolean enabled = false;
		}
	}

	@Data
	public static class Custom {
		/** Spring settings that apply to the webclient only. */
		private Spring spring = new Spring();

		@Data
		public static class Spring {
			/** JSON processing of the webclient REST services. */
			private Jackson jackson = new Jackson();

			@Data
			public static class Jackson {
				/** Parsing of requests. */
				private Parser parser = new Parser();
				/** Serialization of responses. */
				private Serialization serialization = new Serialization();

				@Data
				public static class Parser {
					/** Maximum length in characters of a single JSON string value. */
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
