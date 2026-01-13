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

describe('Process instance variables tests', () => {
  it('add/remove variable', () => {
    cy.loginDefault()

    //cy.get('.d-md-flex > :nth-child(1) > .nav-item > .nav-link').click()
    //cy.wait(5000)

    cy.visit('#/seven/auth/processes/list')

    if (Cypress.env('ENV') === 'stage') {
      // filter test process definition on stage
      cy.get('.form-control-plaintext').type('test')

      // sort ASCENDING by number of instances
      cy.get(':nth-child(2) > .d-flex > .sort-icon > .mdi').click()
      // sort DESCENDING by number of instances
      cy.get('span > .mdi').click()
    }

    // click any first process definition to see its instances
    cy.get(':nth-child(1) > :nth-child(3) > .text-truncate').click()

    // click any first process instance to see its variables
    cy.get(':nth-child(1) > :nth-child(5) > .text-truncate').click()

    // create random variable name
    const variableName = `__cy_test__variable_${Math.floor(Math.random() * 1000000)}`

    // verify table first column has no `variableName` variable
    cy.get('.overflow-y-scroll > .table > tbody').should('not.contain', variableName)

    // click add variable button
    cy.get('.p-3 > .btn').click()

    // fill the form and submit
    const formModal = '.modal.show > .modal-dialog > .modal-content'
    cy.get(formModal).find('input.form-control').type(variableName)
    cy.get(formModal).find('textarea.form-control').type('variable data')
    cy.get(formModal).find('button.btn-primary').click()

    // verify table first column has `variableName` variable
    cy.get('.overflow-y-scroll > .table > tbody').should('contain', variableName)

    // delete the created variable
    // find the row with the variable name and click delete button in that row
    cy.get('.overflow-y-scroll > .table > tbody')
      .contains('tr', variableName)
      .within(() => {
        cy.get('.mdi-delete-outline').click()
      })

    cy.get(formModal).find('button.btn-primary').click()

    // TODO dialog is still shown so, let's reffresh the page
    // refresh the page
    cy.reload()

    // verify table first column has no `variableName` variable
    cy.get('.overflow-y-scroll > .table > tbody').should('not.contain', variableName)

    cy.logout()
  })

})
