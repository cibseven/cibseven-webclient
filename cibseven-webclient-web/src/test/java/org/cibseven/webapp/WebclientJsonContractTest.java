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
package org.cibseven.webapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.model.ElementTemplateOrigin;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.plugin.PluginRegistry;
import org.cibseven.webapp.plugin.PluginService;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.CustomRestTemplate;
import org.cibseven.webapp.rest.InfoService;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Credentials;
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.ProcessVariablesCriteria;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskSorting;
import org.cibseven.webapp.rest.model.Tenant;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.cibseven.webapp.rest.model.Variable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import tools.jackson.databind.json.JsonMapper;

/**
 * Pins, byte for byte, the JSON the webclient backend writes and reads: the bodies of its REST
 * services as Spring MVC writes them with the converters {@link SevenWebclientContext} configures,
 * the request bodies it sends to engine-rest and the engine responses it reads. The JSON the
 * frontend and other clients see must not change when the webclient moves from Jackson 2 to
 * Jackson 3 (CIB7-1535).
 * <p>
 * The fixtures below {@code src/test/resources/json-contract} were recorded on Jackson 2. Only a
 * deliberate change of the wire format may update them: run the test with
 * {@code -Dwebclient.json.contract.record=true}, then review the diff of the fixtures.
 */
class WebclientJsonContractTest {

	private static final Path FIXTURES = Path.of("src", "test", "resources", "json-contract");
	private static final boolean RECORD = Boolean.getBoolean("webclient.json.contract.record");

	private final ContractController controller = new ContractController();
	private final SevenWebclientContext sevenWebclientContext = new SevenWebclientContext(new SevenWebclientProperties());
	private GenericWebApplicationContext webContext;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		webContext = new GenericWebApplicationContext(new MockServletContext());
		AnnotationConfigUtils.registerAnnotationConfigProcessors(webContext);
		webContext.registerBean(MvcConfiguration.class);
		// Registered as a plain WebMvcConfigurer: as a configuration class, its component scan would
		// pull in the whole webclient
		webContext.registerBean("sevenWebclientContext", WebMvcConfigurer.class, () -> sevenWebclientContext);
		webContext.registerBean(ContractController.class, () -> controller);
		webContext.refresh();
		mvc = MockMvcBuilders.webAppContextSetup(webContext).build();
	}

	@AfterEach
	void tearDown() {
		webContext.close();
	}

	// ---------------------------------------------------------------- services of the webclient

	/**
	 * Spring MVC writes and reads JSON with the Jackson 3 converter on the webclient's own mapper: a
	 * Jackson 2 converter would write the Jackson 3 trees the services return as beans.
	 */
	@Test
	void theEffectiveJsonConverterIsJackson3WithTheWebclientsMapper() {
		List<HttpMessageConverter<?>> converters = webContext.getBean(RequestMappingHandlerAdapter.class)
				.getMessageConverters();
		HttpMessageConverter<?> json = converters.stream()
				.filter(converter -> converter.canWrite(Map.class, MediaType.APPLICATION_JSON))
				.findFirst().orElseThrow();

		assertThat(json).isInstanceOf(JacksonJsonHttpMessageConverter.class);
		assertThat(((JacksonJsonHttpMessageConverter) json).getMapper()).isSameAs(sevenWebclientContext.jsonMapper());
		// no Jackson 2 converter behind it that could take over JSON (Spring still registers the Jackson 2
		// YAML one, as swagger-core brings jackson-dataformat-yaml)
		assertThat(converters).filteredOn(converter -> converter.canRead(Map.class, MediaType.APPLICATION_JSON)
				|| converter.canWrite(Map.class, MediaType.APPLICATION_JSON)).containsExactly(json);
	}

	/** {@code GET /info/properties} with the defaults and an engine before 3.0 (nothing reported). */
	@Test
	void infoPropertiesWithDefaults() throws Exception {
		EngineConfiguration engine = new EngineConfiguration();
		engine.setHistoryLevel("full");
		controller.body = () -> infoService(new SevenWebclientProperties(), engine).getConfig(null);

		assertMatchesFixture("info-properties-defaults.json", getBody());
	}

	/** {@code GET /info/properties} with everything set: SSO, support dialog, links, engine TTL. */
	@Test
	void infoPropertiesConfigured() throws Exception {
		SevenWebclientProperties properties = new SevenWebclientProperties();
		properties.setTheme("cib");
		properties.setProductNamePageTitle("CIB seven");
		properties.setSupportDialog("{\"email\":\"support@example.com\",\"phone\":\"+49 89 1234\",\"hours\":[8,17],\"open\":true}");
		properties.getUser().setEditable(false);
		properties.getUser().setUserPasswordChangeEnabled(true);
		properties.getLink().setTerms("https://example.com/terms");
		properties.getLink().setHelp("https://example.com/help");
		properties.getSso().setActive(true);
		properties.getSso().setClientId("webclient");
		properties.getSso().setScopes("openid profile");
		properties.getSso().getEndpoints().setAuthorization("https://sso.example.com/auth");
		properties.getPlugins().setEnabled(true);
		properties.getEngineRest().setUrl("http://engine:8080");
		EngineConfiguration engine = new EngineConfiguration("default", "FULL", true, false, 180, true);
		engine.setWebclientExtensionsEnabled(true);
		controller.body = () -> infoService(properties, engine).getConfig(null);

		assertMatchesFixture("info-properties-configured.json", getBody());
	}

	/** {@code GET /plugins}: the manifests as the registry passes them on. */
	@Test
	void plugins() throws Exception {
		PluginService service = new PluginService(new PluginRegistry(new SevenWebclientProperties()));
		controller.body = service::getPlugins;

		assertMatchesFixture("plugins.json", getBody());
	}

	/** Every date and time type the services return, written with the webclient's settings. */
	@Test
	void datesAndTimes() throws Exception {
		Map<String, Object> dates = new LinkedHashMap<>();
		dates.put("date", new Date(0));
		dates.put("timestamp", new Timestamp(1_700_000_000_123L));
		dates.put("instant", Instant.ofEpochMilli(1_700_000_000_123L));
		dates.put("instantNanos", Instant.ofEpochSecond(1_700_000_000L, 123_456_789));
		dates.put("localDateTime", LocalDateTime.of(2026, 11, 7, 12, 30, 15, 123_000_000));
		dates.put("localDateTimeWholeSecond", LocalDateTime.of(2026, 11, 7, 12, 30));
		dates.put("localDate", LocalDate.of(2026, 11, 7));
		dates.put("localTime", LocalTime.of(12, 30, 15));
		dates.put("offsetDateTime", OffsetDateTime.of(2026, 11, 7, 12, 30, 15, 0, ZoneOffset.ofHours(2)));
		dates.put("zonedDateTime", ZonedDateTime.of(2026, 7, 1, 12, 0, 0, 0, ZoneId.of("Europe/Berlin")));
		dates.put("duration", Duration.ofHours(36).plusMillis(5));
		dates.put("nullDate", null);
		controller.body = () -> dates;

		assertMatchesFixture("dates.json", getBody());
	}

	/** The modeler's entities, with their {@code Timestamp} and {@code LocalDateTime} columns. */
	@Test
	void modelerEntities() throws Exception {
		ProcessDiagramEntity diagram = new ProcessDiagramEntity();
		diagram.setId("diagram-1");
		diagram.setName("Invoice");
		diagram.setProcesskey("invoice");
		diagram.setDescription("Invoice approval");
		diagram.setCreated(new Timestamp(1_700_000_000_000L));
		diagram.setUpdated(new Timestamp(1_700_000_100_987L));
		diagram.setUpdatedBy("demo");
		diagram.setFolderId("folder-1");
		diagram.setVersion(3);
		diagram.setDiagram("<bpmn:definitions/>".getBytes(StandardCharsets.UTF_8));

		ElementTemplate template = new ElementTemplate();
		template.setId("4a7f");
		template.setTemplateId("io.cibseven.mail");
		template.setName("Mail");
		template.setOrigin(ElementTemplateOrigin.values()[0]);
		template.setContent("{\"id\":\"io.cibseven.mail\"}");
		template.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5, 600_000_000));
		template.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
		template.setCreatedBy("system");

		List<Object> entities = new ArrayList<>();
		entities.add(diagram);
		entities.add(template);
		controller.body = () -> entities;

		assertMatchesFixture("modeler-entities.json", getBody());
	}

	/** A tree passed through as is, like the engine's telemetry data. */
	@Test
	void treePassThrough() throws Exception {
		String telemetry = "{\"installation\":\"8343cc7a\",\"product\":{\"name\":\"CIB seven\",\"version\":\"3.0.0\","
				+ "\"internals\":{\"database\":{\"vendor\":\"h2\"},\"commands\":{\"StartProcessInstanceCmd\":{\"count\":12345678901}},"
				+ "\"metrics\":{\"root-process-instance-start\":{\"count\":0}},\"jdk\":{\"version\":\"17.0.12\"},"
				+ "\"telemetryEnabled\":null,\"ratio\":0.5,\"webapps\":[\"cockpit\",\"tasklist\"]}}}";
		Object tree = readTree(telemetry);
		controller.body = () -> tree;

		assertMatchesFixture("tree-pass-through.json", getBody());
	}

	/**
	 * A filter as the frontend sends it, read and written back: primitives sent as {@code null},
	 * properties the webclient does not know, and the criteria trees passed on to the engine.
	 */
	@Test
	void filterRoundTrip() throws Exception {
		assertMatchesFixture("filter-round-trip.json", postBody("/contract/filter", FILTER));
	}

	/** A modeler diagram as the frontend sends it: the primitive version as {@code null}, the diagram as base64. */
	@Test
	void diagramRoundTrip() throws Exception {
		String diagram = "{\"id\":\"diagram-1\",\"name\":\"Invoice\",\"processkey\":\"invoice\",\"version\":null,"
				+ "\"active\":null,\"diagram\":\"PGJwbW46ZGVmaW5pdGlvbnMvPg==\",\"created\":\"2026-11-07T12:30:15.123+0000\","
				+ "\"updated\":1700000000000,\"unknown\":{\"nested\":true}}";

		assertMatchesFixture("diagram-round-trip.json", postBody("/contract/diagram", diagram));
	}

	/**
	 * An element template as it is uploaded: its own {@code id} is the template id, the read-only
	 * properties are ignored.
	 */
	@Test
	void elementTemplateRoundTrip() throws Exception {
		String template = "{\"id\":\"io.cibseven.mail\",\"name\":\"Mail\",\"description\":\"Sends a mail\","
				+ "\"active\":false,\"version\":7,\"origin\":\"USER\",\"createdAt\":\"2020-01-01T00:00:00\","
				+ "\"content\":\"{}\",\"$schema\":\"https://unpkg.com/@camunda/element-templates-json-schema/resources/schema.json\"}";

		assertMatchesFixture("element-template-round-trip.json", postBody("/contract/template", template));
	}

	/** Untyped variable values: number sizes, nesting, nulls, escapes and characters outside the BMP survive as they came. */
	@Test
	void variableRoundTrip() throws Exception {
		String variable = "{\"name\":\"order\",\"type\":\"Json\",\"value\":{\"int\":1,\"long\":12345678901,"
				+ "\"big\":123456789012345678901234567890,\"double\":1.5,\"exp\":1.0E-7,\"list\":[true,null,\"x\",{}],"
				+ "\"text\":\"Größe – 姓名 \\u2028 😀 \\uD83D\\uDE00\",\"control\":\"tab\\tnewline\\n\\u0001\"},\"valueInfo\":{\"filename\":\"order.json\",\"mimeType\":\"application/json\"}}";

		assertMatchesFixture("variable-round-trip.json", postBody("/contract/variable", variable));
	}

	// ------------------------------------------------------------------------------ engine-rest

	/** The engine configuration as engine builds report it, read by the engine REST client. */
	@Test
	void engineConfigurationAsReportedByTheEngine() throws Exception {
		String[] historyTimeToLives = { "180", "\"P180D\"", "\" 30 \"", "\"abc\"", "\"\"", "true", "{\"days\":1}",
				"[1,2]", "1.5", "9999999999", "null", "-1" };
		List<EngineConfiguration> read = new ArrayList<>();
		for (String historyTimeToLive : historyTimeToLives) {
			read.add(readFromEngine("{\"name\":\"default\",\"historyLevel\":\"FULL\",\"authorizationEnabled\":false,"
					+ "\"enablePasswordPolicy\":true,\"historyTimeToLive\":" + historyTimeToLive
					+ ",\"enforceHistoryTimeToLive\":false,\"webclientExtensionsVersion\":\"1.0\",\"newProperty\":42}",
					EngineConfiguration.class));
		}
		// before 3.0.0: none of the new properties
		read.add(readFromEngine("{\"historyLevel\":\"audit\",\"authorizationEnabled\":true}", EngineConfiguration.class));
		// tables not there: the version is sent as null
		read.add(readFromEngine("{\"webclientExtensionsVersion\":null}", EngineConfiguration.class));
		controller.body = () -> read;

		assertMatchesFixture("engine-configuration.json", getBody());
	}

	/** Engine responses with the aliases and primitives the models map. */
	@Test
	void engineTasks() throws Exception {
		String tasks = "[{\"id\":\"task-1\",\"name\":\"Approve\",\"assignee\":null,\"priority\":50,\"created\":\"2026-11-07T12:30:15.123+0100\","
				+ "\"due\":null,\"taskDefinitionKey\":\"approve\",\"processDefinitionId\":\"invoice:1:7\",\"suspended\":false,"
				+ "\"camundaFormRef\":null,\"formKey\":\"embedded:app:forms/approve.html\",\"unknown\":[1]},"
				+ "{\"id\":\"task-2\",\"creationDate\":\"2026-11-07T00:00:00.000+0000\",\"dueDate\":\"2026-11-08T00:00:00.000+0000\","
				+ "\"followUpDate\":\"2026-11-09T00:00:00.000+0000\",\"taskDefinitionId\":\"review\",\"processDefinitionKey\":\"invoice\","
				+ "\"processInstanceKey\":\"pi-1\",\"priority\":0}]";
		Task[] read = readFromEngine(tasks, Task[].class);
		controller.body = () -> read;

		assertMatchesFixture("engine-tasks.json", getBody());
	}

	/** A request body the engine REST client sends: dates, a filter with its criteria trees, a variable. */
	@Test
	void engineRequestBody() throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("date", new Date(1_700_000_000_123L));
		body.put("localDate", LocalDate.of(2026, 11, 7));
		body.put("filter", readWithMvc(FILTER, Filter.class));
		Variable variable = new Variable();
		variable.setName("amount");
		variable.setType("Double");
		variable.setValue(30.5);
		body.put("variable", variable);

		assertMatchesFixture("engine-request-body.json", sendToEngine(body));
	}

	/**
	 * The {@code json()} representations some providers send to engine-rest as the request body,
	 * built with an unconfigured mapper.
	 */
	@Test
	void modelJson() throws Exception {
		Task task = new Task();
		task.setId("task-1");
		task.setPriority(50);
		task.setVariables(new LinkedHashMap<>(Map.of("a", 1)));
		Authorization authorization = new Authorization("auth-1", 1, new String[] { "READ", "UPDATE" }, "demo", null, 6, "*");
		TaskFiltering filtering = new TaskFiltering();
		filtering.setSorting(List.of(new TaskSorting("created", "desc")));
		filtering.setProcessVariables(List.of(new ProcessVariablesCriteria("amount", "gt", 100)));
		filtering.setActive(true);
		filtering.setVariableNames(List.of("amount"));
		filtering.setDueAfter("2026-11-07T00:00:00.000+0000");

		StringBuilder json = new StringBuilder();
		json.append("Task ").append(task.json()).append('\n');
		json.append("Authorization ").append(authorization.json()).append('\n');
		json.append("TaskFiltering ").append(filtering.json()).append('\n');
		json.append("TaskSorting ").append(new TaskSorting("name", "asc").json()).append('\n');
		json.append("ProcessVariablesCriteria ").append(new ProcessVariablesCriteria("x", "eq", null).json()).append('\n');
		json.append("User ").append(new User("demo", "Demo", "Demo", "demo@example.com").json()).append('\n');
		json.append("Credentials ").append(new Credentials("secret").json()).append('\n');
		json.append("Tenant ").append(new Tenant("tenant-1", "Tenant 1").json()).append('\n');
		json.append("UserGroup ").append(new UserGroup("sales", "Sales", "WORKFLOW").json()).append('\n');
		json.append("IdentityLink ").append(new IdentityLink("demo", null, "candidate").json()).append('\n');
		json.append("Engine ").append(new Engine("default").json()).append('\n');
		json.append("Filter ").append(readWithMvc(FILTER, Filter.class).json()).append('\n');

		assertMatchesFixture("model-json.txt", json.toString());
	}

	// --------------------------------------------------------------------------------- helpers

	private static final String FILTER = "{\"id\":\"filter-1\",\"resourceType\":\"Task\",\"name\":\"My tasks\",\"owner\":null,"
			+ "\"query\":{\"assigneeExpression\":\"${currentUser()}\",\"assigned\":null,\"active\":true,"
			+ "\"candidateGroupsExpression\":\"${currentUserGroups()}\",\"processVariables\":[{\"name\":\"amount\",\"operator\":\"gt\",\"value\":100}],"
			+ "\"taskVariables\":[],\"orQueries\":[{\"candidateUser\":\"demo\",\"includeAssignedTasks\":true}],"
			+ "\"sorting\":[{\"sortBy\":\"created\",\"sortOrder\":\"desc\"}],\"followUpBeforeOrNotExistentExpression\":\"${now()}\","
			+ "\"unknownCriterion\":\"x\"},"
			+ "\"properties\":{\"color\":\"#555555\",\"showUndefinedVariable\":null,\"refresh\":true,\"priority\":null,"
			+ "\"description\":\"Mine\",\"variables\":[{\"name\":\"amount\",\"label\":\"Amount\"}],\"foo\":1},\"unknown\":true}";

	private InfoService infoService(SevenWebclientProperties properties, EngineConfiguration engine) {
		BpmProvider bpmProvider = mock(BpmProvider.class);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(engine);
		InfoService service = new InfoService();
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(service, "properties", properties);
		ReflectionTestUtils.setField(service, "authorizationEnabled", true);
		return service;
	}

	private String getBody() throws Exception {
		return mvc.perform(get("/contract/body").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	private String postBody(String path, String json) throws Exception {
		return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.content(json.getBytes(StandardCharsets.UTF_8)))
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	private <T> T readWithMvc(String json, Class<T> type) throws Exception {
		controller.body = null;
		postBody("/contract/filter", json);
		return type.cast(controller.received);
	}

	private <T> T readFromEngine(String json, Class<T> type) {
		CustomRestTemplate template = new SevenWebclientContext(new SevenWebclientProperties()).customRestTemplate();
		MockRestServiceServer engine = MockRestServiceServer.bindTo(template).build();
		engine.expect(requestTo("http://engine/engine-rest/resource"))
				.andRespond(withSuccess(json.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON));
		T read = template.getForObject("http://engine/engine-rest/resource", type);
		engine.verify();
		return read;
	}

	private String sendToEngine(Object body) {
		CustomRestTemplate template = new SevenWebclientContext(new SevenWebclientProperties()).customRestTemplate();
		MockRestServiceServer engine = MockRestServiceServer.bindTo(template).build();
		StringBuilder sent = new StringBuilder();
		engine.expect(requestTo("http://engine/engine-rest/resource"))
				.andExpect(request -> sent.append(((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8)))
				.andRespond(withSuccess());
		template.exchange("http://engine/engine-rest/resource", HttpMethod.POST,
				new org.springframework.http.HttpEntity<>(body, jsonHeaders()), Void.class);
		engine.verify();
		return sent.toString();
	}

	private static org.springframework.http.HttpHeaders jsonHeaders() {
		org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	private static Object readTree(String json) {
		return JsonMapper.builderWithJackson2Defaults().build().readTree(json);
	}

	private static void assertMatchesFixture(String name, String actual) throws IOException {
		Path fixture = FIXTURES.resolve(name);
		if (RECORD) {
			Files.createDirectories(FIXTURES);
			Files.writeString(fixture, actual, StandardCharsets.UTF_8);
		}
		assertThat(actual).isEqualTo(Files.readString(fixture, StandardCharsets.UTF_8));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class MvcConfiguration {
	}

	/** Hands the values under test to Spring MVC, the same way the webclient's services do. */
	@RestController
	static class ContractController {

		Supplier<Object> body;
		Object received;

		@GetMapping("/contract/body")
		Object body() {
			return body.get();
		}

		@PostMapping("/contract/filter")
		Filter filter(@RequestBody Filter filter) {
			received = filter;
			return filter;
		}

		@PostMapping("/contract/diagram")
		ProcessDiagramEntity diagram(@RequestBody ProcessDiagramEntity diagram) {
			return diagram;
		}

		@PostMapping("/contract/template")
		ElementTemplate template(@RequestBody ElementTemplate template) {
			return template;
		}

		@PostMapping("/contract/variable")
		Variable variable(@RequestBody Variable variable) {
			return variable;
		}
	}
}
