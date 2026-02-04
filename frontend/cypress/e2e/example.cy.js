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
// https://on.cypress.io/api

Cypress.Commands.add('loginFail', (email, password) => {
  cy.visit('#/seven/login')
  cy.get(':nth-child(3) > form > :nth-child(1) > .row > .col > .form-control').type(email)
  cy.get('.input-group > .form-control').type(password)
  cy.get('button[type=submit]').click()

  cy.contains(/Authentication data is wrong|Username or password is incorrect/).should('be.visible')
  //cy.get('.modal.show .modal-dialog .modal-content .modal-footer .btn').click()
  //cy.contains('Authentication data is wrong').should('not.be.visible')
})


describe('Simple tests', () => {
  it('visits the app root url', () => {
    cy.visit('/')
    cy.contains('h1', 'CIB seven')
  })

  it('login', () => {
    if (Cypress.env('ENV') === 'stage') {
      cy.loginDefault()
      cy.logout()
    }
    else {
      cy.login('demo', 'demo')
      cy.logout()

      cy.login('mary', 'mary', 'Mary Anne')
      cy.logout()

      cy.login('john', 'john', "John Doe")
      cy.logout()
    }
  })

  it('failed login 1', () => {
    cy.loginFail('demo', 'wrong password')
  })

  it('failed login 2', () => {
    cy.loginFail('demo-non-existing', 'demo')
  })
})
