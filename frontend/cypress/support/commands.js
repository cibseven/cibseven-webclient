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
// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
Cypress.Commands.add('loginDefault', () => {
  console.log('Cypress.env(\'ENV\')=', Cypress.env('ENV'))
  if (Cypress.env('ENV') === 'stage') {
    const username = Cypress.env('username')
    const password = Cypress.env('password')
    cy.login(username, password)
  }
  else {
    cy.login('demo', 'demo')
  }
})

Cypress.Commands.add('login', (username, password, displayName) => {
  cy.visit('#/seven/login')
  cy.get(':nth-child(3) > form > :nth-child(1) > .row > .col > .form-control').type(username)
  cy.get('.input-group > .form-control').type(password, { log: false })
  cy.get('button[type=submit]').click()

  // capitalize username for user name display
  const capitalize = (s) => s && s[0].toUpperCase() + s.slice(1)
  displayName = displayName || capitalize(username)

  // check that we are logged in
  // Wait for the user account dropdown with the account icon to be visible
  cy.get('.mdi-account', { timeout: 10000 })
    .parent('a.nav-link.dropdown-toggle')
    .should('be.visible')
    // check that the user name is correct
    .and('contain', displayName)
})

Cypress.Commands.add('logout', () => {
  cy.get('.mdi-account').parent('a.nav-link.dropdown-toggle').click()
  cy.get('.dropdown-menu-end .dropdown-item').contains('Logout').click()
  cy.get('button[type=submit]').should('contain', 'Login')

  cy.contains('h1', 'CIB seven')
})

//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
