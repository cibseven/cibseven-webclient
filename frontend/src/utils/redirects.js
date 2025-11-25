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
import { HistoryService, ProcessService } from '@/services.js'

async function getHistoryProcessInstanceData(instanceId) {
  return HistoryService.findProcessInstance(instanceId).then(instanceData => {
    return {
      processKey: instanceData.processDefinitionKey,
      versionIndex: instanceData.processDefinitionVersion,
      tenantId: instanceData.tenantId,
    }
  })
}

async function getRuntimeProcessInstanceData(instanceId) {
  return ProcessService.findProcessInstance(instanceId).then(async instanceData => {
    const processDefinition = await ProcessService.findProcessById(instanceData.definitionId, false)
    return {
      processKey: processDefinition.key,
      versionIndex: processDefinition.version,
      tenantId: instanceData.tenantId,
    }
  })
}

export async function redirectToProcessInstance(router, to, from, next) {
  const instanceId = to.params.instanceId
  const cockpitAvailable = router.root.applicationPermissions(router.root.config.permissions['cockpit'], 'cockpit')
  if (cockpitAvailable) {
    const historyLevel = router.root.config.camundaHistoryLevel || 'none'
    const method = historyLevel !== 'none' ? getHistoryProcessInstanceData : getRuntimeProcessInstanceData
    await method(instanceId).then(instanceData => {
      next({
        name: 'process',
        params: {
          processKey: instanceData.processKey,
          versionIndex: instanceData.versionIndex,
          instanceId,
        },
        query: {
          ...to.query,
          ...(instanceData.tenantId ? { tenantId: instanceData.tenantId } : {}),
          tab: to.query?.tab || 'variables',
        }
      })
    }).catch(() => {
      next({
        name: 'not-found-instanceId',
        query: {
          instanceId,
          refPath: from.fullPath,
        }
      })
    })
  }
  else {
    next({
      name: 'start',
    })
  }
}
