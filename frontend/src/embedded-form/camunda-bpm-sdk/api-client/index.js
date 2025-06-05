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

'use strict';
import Events from './../events.js';
import HttpClient from './http-client.js';
import authorizationResource from './resources/authorization.js';
import batchResource from './resources/batch.js';
import deploymentResource from './resources/deployment.js';
import externalTaskResource from './resources/external-task.js';
import filterResource from './resources/filter.js';
import historyResource from './resources/history.js';
import processDefinitionResource from './resources/process-definition.js';
import processInstanceResource from './resources/process-instance.js';
import taskResource from './resources/task.js';
import taskReportResource from './resources/task-report.js';
import telemetryResource from './resources/telemetry.js';
import variableResource from './resources/variable.js';
import caseExecutionResource from './resources/case-execution.js';
import caseInstanceResource from './resources/case-instance.js';
import caseDefinitionResource from './resources/case-definition.js';
import userResource from './resources/user.js';
import groupResource from './resources/group.js';
import tenantResource from './resources/tenant.js';
import incidentResource from './resources/incident.js';
import jobDefinitionResource from './resources/job-definition.js';
import jobResource from './resources/job.js';
import metricsResource from './resources/metrics.js';
import decisionDefinitionResource from './resources/decision-definition.js';
import executionResource from './resources/execution.js';
import migrationResource from './resources/migration.js';
import drdResource from './resources/drd.js';
import modificationResource from './resources/modification.js';
import messageResource from './resources/message.js';
import passwordPolicyResource from './resources/password-policy.js';

/**
 * For all API client related
 * @namespace CamSDK.client
 */

/**
 * For the resources implementations
 * @namespace CamSDK.client.resource
 */

/**
 * Entry point of the module
 *
 * @class CamundaClient
 * @memberof CamSDK.client
 *
 * @param  {Object} config                  used to provide necessary configuration
 * @param  {String} [config.engine=default] false to define absolute apiUri
 * @param  {String} config.apiUri
 * @param  {String} [config.headers]        Headers that should be used for all Http requests.
 */
function CamundaClient(config) {
  if (!config) {
    throw new Error('Needs configuration');
  }

  if (!config.apiUri) {
    throw new Error('An apiUri is required');
  }

  Events.attach(this);

  // use 'default' engine
  config.engine =
    typeof config.engine !== 'undefined' ? config.engine : 'default';

  // mock by default.. for now
  config.mock = typeof config.mock !== 'undefined' ? config.mock : true;

  config.resources = config.resources || {};

  this.HttpClient = config.HttpClient || CamundaClient.HttpClient;

  this.baseUrl = config.apiUri;
  if (config.engine) {
    this.baseUrl += this.baseUrl.slice(-1) !== '/' ? '/' : '';
    this.baseUrl += 'engine/' + config.engine;
  }

  this.config = config;

  this.initialize();
}

/**
 * [HttpClient description]
 * @memberof CamSDK.client.CamundaClient
 * @name HttpClient
 * @type {CamSDK.client.HttpClient}
 */
CamundaClient.HttpClient = HttpClient;

// provide an isolated scope
(function(proto) {
  /**
   * configuration storage
   * @memberof CamSDK.client.CamundaClient.prototype
   * @name  config
   * @type {Object}
   */
  proto.config = {};

  var _resources = {};

  /**
   * @memberof CamSDK.client.CamundaClient.prototype
   * @name initialize
   */
  proto.initialize = function() {
    /* jshint sub: true */
    _resources['authorization'] = authorizationResource;
    _resources['batch'] = batchResource;
    _resources['deployment'] = deploymentResource;
    _resources['external-task'] = externalTaskResource;
    _resources['filter'] = filterResource;
    _resources['history'] = historyResource;
    _resources['process-definition'] = processDefinitionResource;
    _resources['process-instance'] = processInstanceResource;
    _resources['task'] = taskResource;
    _resources['task-report'] = taskReportResource;
    _resources['telemetry'] = telemetryResource;
    _resources['variable'] = variableResource;
    _resources['case-execution'] = caseExecutionResource;
    _resources['case-instance'] = caseInstanceResource;
    _resources['case-definition'] = caseDefinitionResource;
    _resources['user'] = userResource;
    _resources['group'] = groupResource;
    _resources['tenant'] = tenantResource;
    _resources['incident'] = incidentResource;
    _resources['job-definition'] = jobDefinitionResource;
    _resources['job'] = jobResource;
    _resources['metrics'] = metricsResource;
    _resources['decision-definition'] = decisionDefinitionResource;
    _resources['execution'] = executionResource;
    _resources['migration'] = migrationResource;
    _resources['drd'] = drdResource;
    _resources['modification'] = modificationResource;
    _resources['message'] = messageResource;
    _resources['password-policy'] = passwordPolicyResource;

    /* jshint sub: false */
    var self = this;

    function forwardError(err) {
      self.trigger('error', err);
    }

    // create global HttpClient instance
    this.http = new this.HttpClient({
      baseUrl: this.baseUrl,
      headers: this.config.headers
    });

    // configure the client for each resources separately,
    var name, conf, resConf, c;
    for (name in _resources) {
      conf = {
        name: name,
        // use the SDK config for some default values
        mock: this.config.mock,
        baseUrl: this.baseUrl,
        headers: this.config.headers
      };
      resConf = this.config.resources[name] || {};

      for (c in resConf) {
        conf[c] = resConf[c];
      }

      // instanciate a HTTP client for the resource
      _resources[name].http = new this.HttpClient(conf);

      // forward request errors
      _resources[name].http.on('error', forwardError);
    }
  };

  /**
   * Allows to get a resource from SDK by its name
   * @memberof CamSDK.client.CamundaClient.prototype
   * @name resource
   *
   * @param  {String} name
   * @return {CamSDK.client.AbstractClientResource}
   */
  proto.resource = function(name) {
    return _resources[name];
  };
})(CamundaClient.prototype);

export default CamundaClient;

/**
 * A [universally unique identifier]{@link en.wikipedia.org/wiki/Universally_unique_identifier}
 * @typedef {String} uuid
 */

/**
 * This callback is displayed as part of the Requester class.
 * @callback requestCallback
 * @param {?Object} error
 * @param {CamSDK.AbstractClientResource|CamSDK.AbstractClientResource[]} [results]
 */

/**
 * Function who does not perform anything
 * @callback noopCallback
 */
