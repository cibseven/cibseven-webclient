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
package org.cibseven.webapp.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskProviderTest {

	private MockEngineRest engine;
	private TaskProvider taskProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		taskProvider = engine.wire(new TaskProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	// ---------- lookup ----------

	@Test
	void findTasks_sortsNewestFirstAndAppendsTheFilter() throws Exception {
		engine.enqueueJson("[{\"id\":\"task-1\",\"name\":\"Approve invoice\"}]");

		Collection<Task> tasks = taskProvider.findTasks("&assignee=demo", user);

		assertThat(tasks).hasSize(1);
		assertThat(tasks.iterator().next().getName()).isEqualTo("Approve invoice");
		assertThat(engine.takePath()).isEqualTo("/task?sortBy=created&sortOrder=desc&assignee=demo");
	}

	@Test
	void findTasks_toleratesAMissingFilter() throws Exception {
		engine.enqueueJson("[]");

		taskProvider.findTasks(null, user);

		assertThat(engine.takePath()).isEqualTo("/task?sortBy=created&sortOrder=desc");
	}

	@Test
	void findTaskById_asksForTheTask() throws Exception {
		engine.enqueueJson("{\"id\":\"task-1\",\"name\":\"Approve invoice\"}");

		Task task = taskProvider.findTaskById("task-1", user);

		assertThat(task.getId()).isEqualTo("task-1");
		assertThat(engine.takePath()).isEqualTo("/task/task-1");
	}

	@Test
	void findTasksByProcessInstance_filtersByInstance() throws Exception {
		engine.enqueueJson("[]");

		taskProvider.findTasksByProcessInstance("pi-1", user);

		assertThat(engine.takePath()).isEqualTo("/task?processInstanceId=pi-1");
	}

	@Test
	void findTasksCount_postsTheFilterAndReadsTheCount() throws Exception {
		engine.enqueueJson("{\"count\":5}");

		Integer count = taskProvider.findTasksCount(new HashMap<>(), user);

		assertThat(count).isEqualTo(5);
		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).endsWith("/task/count");
	}

	@Test
	void findTasksByProcessInstanceAsignee_filtersByTheAuthenticatedUser() throws Exception {
		engine.enqueueJson("[]");

		taskProvider.findTasksByProcessInstanceAsignee(
			Optional.of("pi-1"), Optional.empty(), user);

		String path = engine.takePath();
		assertThat(path).startsWith("/task?");
		assertThat(path).contains("processInstanceId=pi-1");
	}

	// ---------- assignment ----------

	@Test
	void setAssignee_claimsTheTaskForAUser() throws Exception {
		engine.enqueueJson("");

		taskProvider.setAssignee("task-1", "demo", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).endsWith("/task/task-1/assignee");
		assertThat(request.getBody().readUtf8()).isEqualTo("{ \"userId\": \"demo\" }");
	}

	@Test
	void setAssignee_unclaimsOnTheLiteralStringNull() throws Exception {
		engine.enqueueJson("");

		// the webclient signals "no assignee" with the four characters n-u-l-l, not a null reference
		taskProvider.setAssignee("task-1", "null", user);

		var request = engine.take();
		assertThat(request.getPath()).endsWith("/task/task-1/unclaim");
		assertThat(request.getBody().readUtf8()).isEqualTo("{}");
	}

	@Test
	void setAssignee_throwsOnANullAssigneeReference() throws Exception {
		// KNOWN BUG (pinned, not fixed): the unclaim path is chosen by comparing the assignee to
		// the string "null", so an actual null reference reaches assignee.equals(...) and NPEs
		// instead of unclaiming the task.
		assertThatThrownBy(() -> taskProvider.setAssignee("task-1", null, user))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void update_sendsOnlyTheFieldsThatWereSet() throws Exception {
		engine.enqueueEmpty(204);
		Task task = new Task();
		task.setId("task-1");
		task.setName("new name");
		task.setAssignee("demo");

		taskProvider.update(task, user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/task/task-1");
		String body = request.getBody().readUtf8();
		// unset fields are omitted so the engine does not clear them
		assertThat(body).contains("\"name\":\"new name\"").contains("\"assignee\":\"demo\"");
		assertThat(body).doesNotContain("description").doesNotContain("owner");
	}

	// ---------- submitting ----------

	@Test
	void submit_postsAnEmptyVariableMap() throws Exception {
		engine.enqueueJson("");

		taskProvider.submit("task-1", user);

		var request = engine.take();
		assertThat(request.getPath()).endsWith("/task/task-1/submit-form");
		assertThat(request.getBody().readUtf8()).isEqualTo("{ \"variables\": {} }");
	}

	@Test
	void submit_postsTheGivenFormResultVerbatim() throws Exception {
		engine.enqueueJson("");

		taskProvider.submit("task-1", "{ \"variables\": { \"amount\": { \"value\": 42 } } }", user);

		assertThat(engine.take().getBody().readUtf8())
			.isEqualTo("{ \"variables\": { \"amount\": { \"value\": 42 } } }");
	}

	// ---------- forms ----------

	@Test
	void form_returnsTheTaskFormWhenItHasAKey() throws Exception {
		engine.enqueueJson("{\"key\":\"embedded:app:forms/approve.html\"}");

		Object form = taskProvider.form("task-1", user);

		assertThat(form).isNotInstanceOf(String.class);
		assertThat(engine.takePath()).isEqualTo("/task/task-1/form");
	}

	@Test
	void form_reportsAnEmptyTaskWhenTheFormHasNoKey() throws Exception {
		engine.enqueueJson("{}");

		assertThat(taskProvider.form("task-1", user)).isEqualTo("empty-task");
	}

	@Test
	void formReference_reportsAnEmptyTaskWhenThereIsNoFormReferenceVariable() throws Exception {
		engine.enqueueJson("{}");

		assertThat(taskProvider.formReference("task-1", user)).isEqualTo("empty-task");
		assertThat(engine.takePath()).isEqualTo("/task/task-1/form-variables?variableNames=formReference");
	}

	@Test
	void formReference_returnsTheVariableValue() throws Exception {
		engine.enqueueJson("{\"formReference\":{\"value\":\"my-form\",\"type\":\"String\"}}");

		assertThat(taskProvider.formReference("task-1", user)).isEqualTo("my-form");
	}

	// ---------- history ----------

	@Test
	void findTasksByProcessInstanceHistory_sortsByStartTimeDescending() throws Exception {
		engine.enqueueJson("[{\"id\":\"task-1\"}]");

		Collection<TaskHistory> tasks = taskProvider.findTasksByProcessInstanceHistory("pi-1", user);

		assertThat(tasks).hasSize(1);
		assertThat(engine.takePath())
			.isEqualTo("/history/task?processInstanceId=pi-1&sortBy=startTime&sortOrder=desc");
	}

	@Test
	void findTasksByDefinitionKeyHistory_filtersByBothInstanceAndDefinitionKey() throws Exception {
		engine.enqueueJson("[]");

		taskProvider.findTasksByDefinitionKeyHistory("approve", "pi-1", user);

		assertThat(engine.takePath())
			.isEqualTo("/history/task?processInstanceId=pi-1&taskDefinitionKey=approve");
	}

	@Test
	void findHistoryTasksCount_postsToTheHistoryCountEndpoint() throws Exception {
		engine.enqueueJson("{\"count\":8}");

		assertThat(taskProvider.findHistoryTasksCount(new HashMap<>(), user)).isEqualTo(8);
		assertThat(engine.takePath()).isEqualTo("/history/task/count");
	}

	@Test
	void getTaskCountByCandidateGroup_asksTheReportEndpoint() throws Exception {
		engine.enqueueJson("[{\"groupName\":\"sales\",\"taskCount\":3}]");

		taskProvider.getTaskCountByCandidateGroup(user);

		assertThat(engine.takePath()).isEqualTo("/task/report/candidate-group-count");
	}

	// ---------- filters ----------

	@Test
	void findTasksCountByFilter_postsTheFilteringToTheFilterCountEndpoint() throws Exception {
		engine.enqueueJson("{\"count\":2}");
		TaskFiltering filtering = new TaskFiltering();
		filtering.setActive(Boolean.TRUE);

		assertThat(taskProvider.findTasksCountByFilter("filter-1", user, filtering)).isEqualTo(2);

		var request = engine.take();
		assertThat(request.getPath()).endsWith("/filter/filter-1/count");
		assertThat(request.getBody().readUtf8()).contains("\"active\":true");
	}

	@Test
	void findTasksByFilter_pagesThroughTheFilterList() throws Exception {
		engine.enqueueJson("[{\"id\":\"task-1\"}]");
		engine.enqueueJson("[]");
		TaskFiltering filtering = new TaskFiltering();
		filtering.setActive(Boolean.TRUE);

		Collection<Task> tasks = taskProvider.findTasksByFilter(filtering, "filter-1", user, 0, 50);

		assertThat(tasks).hasSize(1);
		assertThat(engine.takePath()).isEqualTo("/filter/filter-1/list?firstResult=0&maxResults=50");
	}
}
