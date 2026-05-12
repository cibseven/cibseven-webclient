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
package org.cibseven.modeler.provider;

import java.util.List;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.UnifiedDiagram;

public interface IUnifiedDiagramProvider {

	/**
	 * Returns a paginated, sorted list of all diagrams (processes and forms)
	 * matching the optional keyword and type filter.
	 *
	 * @param keyword    substring to match against name/processkey/formId ('' = no filter)
	 * @param type       exact type to match, e.g. 'bpmn-c7', 'dmn', 'form' ('' = all)
	 * @param firstResult zero-based offset
	 * @param maxResults  page size
	 */
	List<UnifiedDiagram> getDiagrams(String keyword, String type, int firstResult, int maxResults) throws SystemException;

}
