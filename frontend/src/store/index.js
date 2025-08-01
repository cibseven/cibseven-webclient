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

import { createStore } from 'vuex'

import ProcessStore from './ProcessStore.js'
import FilterStore from './FilterStore.js'
import AdvancedSearchStore from './AdvancedSearchStore.js'
import UserStore from './UserStore.js'
import DecisionStore from './DecisionStore.js'
import ActivityStore from './ActivityStore.js'
import BatchStore from './BatchStore.js'
import TenantStore from './TenantStore.js'
import JobStore from './JobStore.js'
import IncidentsStore from './IncidentsStore.js'
import InstancesStore from './InstancesStore.js'
import CalledProcessDefinitionsStore from './CalledProcessDefinitionsStore.js'
import VariableInstanceStore from './VariableInstanceStore.js'
import HistoricVariableInstanceStore from './HistoricVariableInstanceStore.js'
import ExternalTaskStore from './ExternalTaskStore.js'
import DiagramStore from './DiagramStore.js'
import TaskStore from './TaskStore'

export const modules = {
  process: ProcessStore,
  filter: FilterStore,
  advancedSearch: AdvancedSearchStore,
  user: UserStore,
  decision: DecisionStore,
  activity: ActivityStore,
  batch: BatchStore,
  tenant: TenantStore,
  job: JobStore,
  variableInstance: VariableInstanceStore,
  historicVariableInstance: HistoricVariableInstanceStore,
  incidents: IncidentsStore,
  instances: InstancesStore,
  calledProcessDefinitions: CalledProcessDefinitionsStore,
  externalTasks: ExternalTaskStore,
  diagram: DiagramStore,
  task: TaskStore
}

const store = createStore({
  modules
})

export default store
