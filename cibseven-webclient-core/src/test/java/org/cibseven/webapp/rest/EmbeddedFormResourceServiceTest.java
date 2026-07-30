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
package org.cibseven.webapp.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The engine-rest compatible identity paths are a thin alias over {@link AdminService}, so these
 * tests assert the delegation and the query-by-body mapping rather than re-testing the lookup
 * itself: the permission check and the provider call live in {@code AdminService}.
 */
@ExtendWith(MockitoExtension.class)
public class EmbeddedFormResourceServiceTest {

	private static final Optional<String> NONE = Optional.empty();

	@Mock
	private AdminService adminService;

	private EmbeddedFormResourceService service;
	private CIBUser user;

	@BeforeEach
	public void setUp() {
		service = new EmbeddedFormResourceService(adminService);
		user = mock(CIBUser.class);
	}

	@Test
	public void findGroupsDelegatesEveryFilterToAdminService() {
		Collection<UserGroup> expected = List.of(mock(UserGroup.class));
		when(adminService.findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(expected);

		Collection<UserGroup> actual = service.findGroups(
				Optional.of("g1"), Optional.of("Sales"), Optional.of("Sal"), Optional.of("WORKFLOW"),
				Optional.of("demo"), Optional.of("tenant1"), Optional.of("name"), Optional.of("asc"),
				Optional.of("0"), Optional.of("50"), Locale.ENGLISH, user);

		assertSame(expected, actual);
		// member/memberOfTenant are passed through untouched - AdminService owns the percent-decoding
		verify(adminService).findGroups(
				Optional.of("g1"), Optional.of("Sales"), Optional.of("Sal"), Optional.of("WORKFLOW"),
				Optional.of("demo"), Optional.of("tenant1"), Optional.of("name"), Optional.of("asc"),
				Optional.of("0"), Optional.of("50"), Locale.ENGLISH, user);
	}

	@Test
	public void queryGroupsMapsRequestBodyOntoTheSameDelegate() {
		when(adminService.findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(List.of());

		Map<String, Object> body = new HashMap<>();
		body.put("id", "g1");
		body.put("name", "Sales");
		body.put("nameLike", "Sal");
		body.put("type", "WORKFLOW");
		body.put("member", "demo");
		body.put("memberOfTenant", "tenant1");
		body.put("sortBy", "name");
		body.put("sortOrder", "asc");
		body.put("firstResult", "0");
		body.put("maxResults", "50");

		service.queryGroups(body, Locale.ENGLISH, user);

		verify(adminService).findGroups(
				Optional.of("g1"), Optional.of("Sales"), Optional.of("Sal"), Optional.of("WORKFLOW"),
				Optional.of("demo"), Optional.of("tenant1"), Optional.of("name"), Optional.of("asc"),
				Optional.of("0"), Optional.of("50"), Locale.ENGLISH, user);
	}

	@Test
	public void queryGroupsTreatsAMissingBodyAsNoFilters() {
		when(adminService.findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(List.of());

		service.queryGroups(null, Locale.ENGLISH, user);

		verify(adminService).findGroups(NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE,
				Locale.ENGLISH, user);
	}

	@Test
	public void queryGroupsIgnoresUnknownKeysAndCoercesNonStringValues() {
		when(adminService.findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(List.of());

		Map<String, Object> body = new HashMap<>();
		body.put("maxResults", 25);
		body.put("somethingTheSdkInvented", "ignored");

		service.queryGroups(body, Locale.ENGLISH, user);

		verify(adminService).findGroups(NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE,
				Optional.of("25"), Locale.ENGLISH, user);
	}

	@Test
	public void findGroupCountReturnsTheEngineCountShapeForTheQueryParamVariant() {
		when(adminService.findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(List.of(mock(UserGroup.class), mock(UserGroup.class),
						mock(UserGroup.class)));

		Map<String, Integer> count = service.findGroupCount(NONE, NONE, NONE, Optional.of("WORKFLOW"),
				Optional.of("demo"), NONE, Locale.ENGLISH, user);

		assertEquals(Map.of("count", 3), count);
		// counting must apply the same filters, without inheriting any pagination
		verify(adminService).findGroups(NONE, NONE, NONE, Optional.of("WORKFLOW"), Optional.of("demo"),
				NONE, NONE, NONE, NONE, NONE, Locale.ENGLISH, user);
	}

	@Test
	public void queryGroupCountReturnsTheEngineCountShape() {
		when(adminService.findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(List.of(mock(UserGroup.class), mock(UserGroup.class)));

		Map<String, Integer> count = service.queryGroupCount(Map.of("type", "WORKFLOW"), Locale.ENGLISH, user);

		assertEquals(Map.of("count", 2), count);
	}

	@Test
	public void queryGroupCountIsZeroWhenNothingMatches() {
		when(adminService.findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(List.of());

		assertEquals(Map.of("count", 0), service.queryGroupCount(null, Locale.ENGLISH, user));
	}

	@Test
	public void findUsersDelegatesEveryFilterToAdminService() {
		Collection<User> expected = List.of(mock(User.class));
		when(adminService.findUsers(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any(), any())).thenReturn(expected);

		Collection<User> actual = service.findUsers(
				Optional.of("demo"), NONE, NONE, NONE, NONE, NONE, NONE, Optional.of("sales"), NONE, NONE,
				NONE, NONE, NONE, NONE, Optional.of(true), Locale.ENGLISH, user);

		assertSame(expected, actual);
		verify(adminService).findUsers(
				Optional.of("demo"), NONE, NONE, NONE, NONE, NONE, NONE, Optional.of("sales"), NONE, NONE,
				NONE, NONE, NONE, NONE, Optional.of(true), Locale.ENGLISH, user);
	}
}
