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

var INTEGER_PATTERN = /^-?[\d]+$/;

var FLOAT_PATTERN = /^(0|(-?(((0|[1-9]\d*)\.\d+)|([1-9]\d*))))([eE][-+]?[0-9]+)?$/;

var BOOLEAN_PATTERN = /^(true|false)$/;

var DATE_PATTERN = /^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(|\.[0-9]{0,4})$/;

import { XMLValidator } from 'fast-xml-parser';

var isValidXML = function(value) {
  if (!value) return false;
  return XMLValidator.validate(value) === true;
};

var isValidJSON = function(value) {
  try {
    JSON.parse(value);
    return true;
  } catch {
    // Invalid JSON, return false
    return false;
  }
};

var isType = function(value, type) {
  switch (type) {
    case 'Integer':
    case 'Long':
    case 'Short':
      return INTEGER_PATTERN.test(value);
    case 'Float':
    case 'Double':
      return FLOAT_PATTERN.test(value);
    case 'Boolean':
      return BOOLEAN_PATTERN.test(value);
    case 'Date': {
      const str = dateToString(value);
      if (!DATE_PATTERN.test(str)) return false;
      // Check that the date is a valid date (not e.g. 2013-13-23)
      const d = new Date(str);
      // Ensure date is valid and matches input string (no rollover)
      if (isNaN(d.getTime())) return false;
      // Check that the string matches the reconstructed ISO string (to catch rollovers)
      const [year, month, day, hour, min, sec] = str.match(/\d+/g).map(Number);
      if (d.getFullYear() !== year || d.getMonth() + 1 !== month || d.getDate() !== day || d.getHours() !== hour || d.getMinutes() !== min || d.getSeconds() !== sec) return false;
      return true;
    }
    case 'Xml':
      return isValidXML(value);
    case 'Json':
      return isValidJSON(value);
  }
};

var convertToType = function(value, type) {
  if (typeof value === 'string') {
    value = value.trim();
  }

  if (type === 'String' || type === 'Bytes' || type === 'File') {
    return value;
  } else if (isType(value, type)) {
    switch (type) {
      case 'Integer':
      case 'Long':
      case 'Short':
        return Number.parseInt(value, 10);
      case 'Float':
      case 'Double':
        return Number.parseFloat(value);
      case 'Boolean':
        return 'true' === value;
      case 'Date':
        return dateToString(value);
    }
  } else {
    throw new Error("Value '" + value + "' is not of type " + type);
  }
};

/**
 * This reformates the date into a ISO8601 conform string which will mirror the selected date in local format.
 * TODO: Remove this when it is fixed by angularjs
 *
 * @see https://app.camunda.com/jira/browse/CAM-4746
 *
 */
var pad = function(number) {
  return number < 10 ? '0' + number : number;
};

var dateToString = function(date) {
  if (typeof date === 'object' && typeof date.getFullYear === 'function') {
    var year = date.getFullYear(),
      month = pad(date.getMonth() + 1),
      day = pad(date.getDate()),
      hour = pad(date.getHours()),
      min = pad(date.getMinutes()),
      sec = pad(date.getSeconds());

    return year + '-' + month + '-' + day + 'T' + hour + ':' + min + ':' + sec;
  } else {
    return date;
  }
};

export default {
  convertToType,
  isType,
  dateToString
};
