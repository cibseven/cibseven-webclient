/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cibseven.webapp.providers;

import java.util.List;

public class PermissionConstants {

    public static final List<String> READ_ALL = List.of("ALL", "READ");
    public static final List<String> CREATE_ALL = List.of("ALL", "CREATE");
    public static final List<String> UPDATE_ALL = List.of("ALL", "UPDATE");
    public static final List<String> DELETE_ALL = List.of("ALL", "DELETE");
    public static final List<String> SUSPEND_ALL = List.of("ALL", "SUSPEND");
    public static final List<String> CREATE_INSTANCE_ALL = List.of("ALL", "CREATE_INSTANCE");
    public static final List<String> READ_HISTORY_ALL = List.of("ALL", "READ_HISTORY");
    public static final List<String> DELETE_HISTORY_ALL = List.of("ALL", "DELETE_HISTORY");
    public static final List<String> SUSPEND_INSTANCE_ALL = List.of("ALL", "SUSPEND_INSTANCE");
    public static final List<String> READ_INSTANCE_VARIABLE_ALL = List.of("ALL", "READ_INSTANCE_VARIABLE");
    public static final List<String> UPDATE_INSTANCE_VARIABLE_ALL = List.of("ALL", "UPDATE_INSTANCE_VARIABLE");

} 