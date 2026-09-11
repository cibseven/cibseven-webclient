---
name: java-jacoco-coverage
description: Write Java tests and read/raise JaCoCo coverage in this repo. Use when adding backend tests, when a jacoco:check gate fails, when asked to raise Java coverage, or before touching the JaCoCo/surefire/failsafe configuration in any pom.
---

# Java test coverage in cibseven-webclient

How to measure Java coverage here, the four test recipes the suite already uses, the traps that
cost real time, and how the `jacoco:check` gates work.

## Measure it

```bash
# The only reliable command. `-pl '!.'` deselects the root module, whose frontend-maven-plugin
# otherwise runs the whole npm pipeline (npm ci + vitest + lint + vite build).
mvn -B clean verify -pl '!.'
```

Use **`verify`, not `test`**. `mvn test` runs only surefire, so it reports a lower number and
never runs the `*IT` classes or the coverage gates. Both are legitimate, but only `verify`
matches what CI (`mvn -T4 … clean verify`) and the gates see.

The repo-wide figure comes from the aggregate module:

```bash
cat cibseven-coverage-aggregate/target/site/jacoco-aggregate/jacoco.csv
```

```bash
# Per-module table plus the ranked remaining targets
python3 - <<'PY'
import csv, collections
tot=collections.Counter(); rows=[]
for r in csv.DictReader(open('cibseven-coverage-aggregate/target/site/jacoco-aggregate/jacoco.csv')):
    mod=r['GROUP'].split('/')[-1]; lc,lm=int(r['LINE_COVERED']),int(r['LINE_MISSED'])
    for k in ('LINE_MISSED','LINE_COVERED','BRANCH_MISSED','BRANCH_COVERED'): tot[k]+=int(r[k])
    if lm: rows.append((lm,lc,mod.replace('cibseven-',''),r['CLASS']))
t=tot['LINE_COVERED']+tot['LINE_MISSED']
print(f"TOTAL {tot['LINE_COVERED']}/{t} = {100.0*tot['LINE_COVERED']/t:.1f}%")
for lm,lc,mod,cls in sorted(rows,reverse=True)[:20]:
    print(f"  {lm:>4} missed {100.0*lc/(lm+lc):>5.0f}%  {mod}/{cls}")
PY
```

**Rank by absolute uncovered lines, never by percentage.** A 0%-covered 30-line class is noise
next to a 40%-covered 600-line one.

## The four recipes

Pick by what the class under test talks to. All four already exist in the repo — copy the
nearest neighbour rather than inventing a fifth.

### 1. REST-over-HTTP provider (`core/webapp.providers`, extends `SevenProviderBase`)

Use the `MockEngineRest` helper
(`cibseven-webclient-core/src/test/java/org/cibseven/webapp/providers/MockEngineRest.java`).
It wires the provider by reflection — these classes have no constructor injection — and stands a
`MockWebServer` in for the engine. No Spring context, so a whole class runs in ~3 s.

```java
engine = new MockEngineRest();
provider = engine.wire(new DecisionProvider());
user = MockEngineRest.user();
// ...
engine.enqueueJson("[{\"id\":\"dec-1\",\"key\":\"risk\"}]");
Collection<Decision> result = provider.getDecisionDefinitionList(new HashMap<>(), user);
assertThat(engine.takePath()).isEqualTo("/decision-definition");
```

Assert **both** halves: the request that went out (path, method, body, headers) and the object
that came back. The request path is where the bugs are — see `DecisionProviderTest`, which pins a
malformed tenant URL.

A provider with extra `@Value` fields of its own needs them set too (`UserProvider` has
`userProvider` and `wildcard`), or you get an NPE deep inside a URL builder.

Canonical examples: `DecisionProviderTest`, `IncidentProviderTest`, `ProcessProviderTest`.

### 2. Embedded-engine provider (`direct-provider/webapp.providers`)

These take their collaborators in the constructor and reach the engine through
`DirectProviderUtil`. Spy the util, stub the two lookup methods, and mock the engine services:

```java
ProcessEngine processEngine = mock(ProcessEngine.class);
RepositoryService repositoryService = mock(RepositoryService.class);
when(processEngine.getRepositoryService()).thenReturn(repositoryService);
// fluent queries need RETURNS_SELF or every chained call returns null
ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class,
    withSettings().defaultAnswer(RETURNS_SELF));
when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);

ObjectMapper objectMapper = new ObjectMapper();
JacksonConfigurator.configureObjectMapper(objectMapper);

directProviderUtil = Mockito.spy(new DirectProviderUtil());
doReturn(processEngine).when(directProviderUtil).getProcessEngine(any(CIBUser.class));
doReturn(objectMapper).when(directProviderUtil).getObjectMapper(any(CIBUser.class));

provider = new DirectProcessProvider(directProviderUtil, mock(SevenDirectProvider.class));
```

`getProcessEngine`/`getObjectMapper` are `protected`, which works because the tests sit in the
same package. Extra services get mocked the same way (`getRuntimeService`, `getTaskService`,
`getHistoryService`, `getManagementService`, `getIdentityService`, `getFormService`).

Canonical examples: `DirectProcessProviderTest`, `DirectTaskProviderTest`, `DirectBatchProviderTest`.

### 3. REST service / facade (`core/webapp.rest`, `core/modeler.rest`)

Plain Mockito plus `ReflectionTestUtils` for the `@Autowired` and `@Value` fields:

```java
service = new ProcessService();
ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
// the deprecated in-webclient permission check is off unless this is true
ReflectionTestUtils.setField(service, "authorizationEnabled", false);
```

A service whose endpoint takes an `HttpServletRequest` derives the user itself, so mock
`baseUserProvider.checkAuthorization(request, true)` too. Modeler services additionally need
`modelerAccessChecker`.

Canonical examples: `InfoServiceTest` (the original), `ProcessServiceTest`, `ModelerServiceTest`.

### 4. Spring wiring (`webapp.plugin.*`, `modeler.config.*`)

`ApplicationContextRunner`, no full boot. See `PluginAutoConfigurationTest`.

### Reserve `@SpringBootTest` for what genuinely needs a context

The `*IT` classes in `core/webapp/providers` predate the `MockEngineRest` helper. Do not add new
ones — a context test costs seconds where the helper costs milliseconds.

## Traps

- **`mvn test` under-reports.** Only `verify` runs the ITs and the gates. See below for why.
- **Always `clean`.** A stale `target/` gives phantom "cannot find symbol" errors in test
  compilation, and a stale `jacoco.csv` silently reports the previous run's numbers.
- **Never nest mock creation inside `thenReturn(...)`.** `when(q.list()).thenReturn(List.of(mockThing()))`
  fails with `UnfinishedStubbingException`, because the helper stubs a second mock before the
  first stubbing completes. Hoist it:
  ```java
  ProcessDefinition definition = mockDefinition("id-1");   // first
  when(query.list()).thenReturn(List.of(definition));      // then
  ```
  This is by far the most common mistake when writing these tests.
- **Primitive parameters need primitive matchers.** `verify(x).deleteDeployment(any(), any(), anyBoolean(), anyBoolean())`
  throws NPE on unboxing — use `anyString()`/`anyBoolean()` positionally. A broken `verify` also
  poisons the *next* test in the class with `InvalidUseOfMatchers`.
- **Jackson cannot serialise a Mockito mock.** Any provider that passes an engine object straight
  into `convertValue` needs a real entity (e.g. `new UserEntity()`), not a mock.
- **MockWebServer hangs when its queue runs dry.** `MockEngineRest` sets
  `QueueDispatcher.setFailFast(true)` and bounds `takeRequest`, so an unexpected extra request
  fails in milliseconds instead of hanging the build for two minutes. Keep that when writing a
  new MockWebServer fixture. If a test fails this way, the provider makes more round-trips than
  you enqueued — usually incident enrichment or a per-item history count.
- **Drain the setup request.** A fixture whose constructor fetches something (a JWKS, say) leaves
  that request in the queue; call `server.takeRequest()` once in `setUp` or your later
  `takeRequest()` returns the wrong one.
- **`-Werror` is on.** Test code must compile without warnings; touching a `@Deprecated` class
  needs `@SuppressWarnings("deprecation")` on the test class.
- **The enforcer fails the build on dependency convergence.** A new test dependency that drags in
  a conflicting transitive version breaks everything, not just your module.
- **Surefire 3.x spells it `-Dsurefire.failIfNoSpecifiedTests=false`**, not
  `-DfailIfNoSpecifiedTests=false`. With the old name, `-Dtest=...` fails the build on the first
  module that has no matching test.
- **Do not grep build logs for an unanchored `EXIT=`** — the H2 test URLs contain
  `DB_CLOSE_ON_EXIT=false`. Anchor it: `grep '^EXIT='`.

## How the JaCoCo setup works

Root `pom.xml`:

- `prepare-agent` → `jacoco-report` (phase `test`) → **`jacoco-report-integration` (phase `verify`)**
  → `jacoco-check` (phase `verify`).
- The `verify`-phase report is the important one. Failsafe runs the `*IT` classes in
  `integration-test`, which is *after* the `test` phase, and appends to the same `jacoco.exec`.
  Reporting only at `test` throws that data away — it used to, and cost
  `cibseven-webclient-core` more than half its reported coverage.
- The `test`-phase report is kept so a plain `mvn test` still produces something.
- `maven-failsafe-plugin` lives in the root pom and includes only
  `org/cibseven/webapp/providers/**/*IT.java`. `org.cibseven.webapp.rest.TenantProviderIT` and
  `AnalyticsServiceIT` are deliberately outside it: they are dormant live-engine tests that drive
  a real `CustomRestTemplate` against a seeded demo dataset that no longer exists.
  `AnalyticsService` is covered by `AnalyticsServiceTest` instead.

## The gates

Two layers, both enforced at `verify`:

1. **Per module.** Each module's pom sets `jacoco.line.coverage.floor` and
   `jacoco.branch.coverage.floor`; the root pom's inherited `jacoco-check` execution applies them
   as a `BUNDLE` rule. Each carries a comment recording the figure it was measured at.
2. **Repo-wide.** `cibseven-coverage-aggregate` merges every module's exec data
   (`jacoco:merge`), unpacks the jar modules' classes so `jacoco:check` has a subject, and
   enforces `jacoco.aggregate.line.floor` / `jacoco.aggregate.branch.floor`. This is the layer
   that notices slow drift, because the merged data also credits cross-module coverage.

**Raising them:** after adding tests, read the new figures and set each floor ~2 points under.
`mvn verify` prints `All coverage checks have been met` once per gate (six today).

**Never lower a floor to make a build pass.** A failing gate means coverage went down; add the
missing test. If a floor is genuinely wrong, say so explicitly in the PR.

To see a gate bite:

```bash
mvn -B clean verify -pl '!.' -Djacoco.aggregate.line.floor=0.99
# Rule violated for bundle cibseven-coverage-aggregate: lines covered ratio is 0.41 …
```

## House conventions

- **Apache-2.0 header on every new file.** Copy the 16-line block from a neighbour verbatim.
- `*Test.java` for unit tests, `*IT.java` only for something that genuinely needs a Spring
  context — and read the failsafe note above first.
- Test packages mirror the main source (`org.cibseven.webapp.*`, `org.cibseven.modeler.*`).
- Several providers already have more than one test class (`DirectUserProviderAuthorizationTest`,
  `DirectUserProviderCrudTest`); splitting by concern is preferred over one huge class.
- Reuse the existing helpers rather than re-inventing them: `MockEngineRest`, `BaseHelper`
  (`loadMockResponse`, `getCibUser`), `MockUserProviderTestConfiguration`,
  `TestRestTemplateConfiguration`, `SevenProviderBaseTest`.
- **Assert behaviour.** `AGENTS.md` forbids assertion-free or padding tests, and a test that only
  checks `isNotNull()` on a mapped object is padding.

## When a test disagrees with the code

Decide which one is wrong before "fixing" the test. If the code is wrong, **pin the current
behaviour** and report it rather than changing production source:

```java
// KNOWN BUG (pinned, not fixed): <what is wrong, and what it costs in production>
assertThat(body).contains("\"executionDate\": 2026-01-01T10:00:00");
```

Existing pins worth knowing about, because they will surprise you:

- `ApplicationException` (and `AuthenticationException` from the shared `common-auth` library)
  declare only a varargs `Object...` constructor and never call `super(message)`. **Every**
  exception in those families has a null `getMessage()` and a broken cause chain; the real
  message is only in `getData()`. Do not assert on the message of one of those.
- `ProcessProvider.suspendProcessDefinition` interpolates the execution date into JSON unquoted.
- `DecisionProvider.evaluateDecisionDefinitionByKeyAndTenant` builds `/tenant<id>` instead of
  `/tenant-id/<id>`.
- `DirectBatchProvider.deleteBatch` compares the cascade flag with `equals("true")`, so a real
  JSON boolean never cascades.
