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

'use strict';

import AbstractClientResource from '../abstract-client-resource.js';
import utils from '../../utils.js';

/**
 * CaseInstance Resource
 * @class
 * @memberof CamSDK.client.resource
 * @augments CamSDK.client.AbstractClientResource
 */
var CaseInstance = AbstractClientResource.extend();

/**
 * Path used by the resource to perform HTTP queries
 * @type {String}
 */
CaseInstance.path = 'case-instance';

CaseInstance.get = function(instanceId, done) {
  return this.http.get(this.path + '/' + instanceId, {
    done: done
  });
};

CaseInstance.list = function(params, done) {
  return this.http.get(this.path, {
    data: params,
    done: done
  });
};

CaseInstance.close = function(instanceId, params, done) {
  return this.http.post(this.path + '/' + instanceId + '/close', {
    data: params,
    done: done
  });
};

CaseInstance.terminate = function(instanceId, params, done) {
  return this.http.post(this.path + '/' + instanceId + '/terminate', {
    data: params,
    done: done
  });
};

/**
 * Sets a variable of a given case instance by id.
 *
 * @see http://docs.camunda.org/manual/develop/reference/rest/case-instance/variables/put-variable/
 *
 * @param   {uuid}              id
 * @param   {Object}            params
 * @param   {requestCallback}   done
 */
CaseInstance.setVariable = function(id, params, done) {
  var url = this.path + '/' + id + '/variables/' + utils.escapeUrl(params.name);
  return this.http.put(url, {
    data: params,
    done: done
  });
};

export default CaseInstance;
