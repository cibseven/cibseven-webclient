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

import constants from '../constants.js';
import AbstractFormField from './abstract-form-field.js';

/**
 * A field control handler for file downloads
 * @class
 * @memberof CamSDK.form
 * @augments {CamSDK.form.AbstractFormField}
 */
var ErrorButtonHandler = AbstractFormField.extend(
  {
    /**
     * Prepares an instance
     */
    initialize: function() {
      this.errorCode = this.element.attr(constants.DIRECTIVE_CAM_ERROR_CODE);
      this.errorMessage = this.element.attr(
        constants.DIRECTIVE_CAM_ERROR_MESSAGE
      );
    },

    applyValue: function() {
      var self = this;
      this.element.on('click', function() {
        self.form.error(self.errorCode, self.errorMessage);
      });

      return this;
    }
  },

  {
    selector: 'button[' + constants.DIRECTIVE_CAM_ERROR_CODE + ']'
  }
);

export default ErrorButtonHandler;
