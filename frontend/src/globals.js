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
import axios from 'axios'
import moment from 'moment'

function createUUID() { //keycloak.js unique identifier for state and nonce
    const s = [];
    const hexDigits = '0123456789abcdef';
    const randomValues = new Uint8Array(36);
    crypto.getRandomValues(randomValues);
    for (let i = 0; i < 36; i++) {
        s[i] = hexDigits.substr(randomValues[i] % 0x10, 1);
    }
    s[14] = '4';
    s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
    s[8] = s[13] = s[18] = s[23] = '-';
    const uuid = s.join('');
    return uuid;
}

export { axios, moment, createUUID }
