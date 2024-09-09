/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('Achieved Sills Progress Tests', () => {

  it('multiple subject with different number of skills', () => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 2, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 3, { numPerformToCompletion: 1 })
    cy.createSubject(1, 2)
    cy.createSkill(1, 2, 4, { numPerformToCompletion: 1 })
    cy.createSkill(1, 2, 5, { numPerformToCompletion: 1 })
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 2, userId: Cypress.env('proxyUser'), date: 'now' })

    cy.cdVisit('/')
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '3')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '5')

    cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click()
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '2')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '3')

    cy.get('[data-cy="breadcrumb-proj1"]').click()
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '3')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '5')

    cy.get('[data-cy="subjectTile-subj2"] [data-cy="subjectTileBtn"]').click()
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '1')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '2')

    cy.cdVisit('/subjects/subj1')
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '2')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '3')
    cy.get('[data-cy="skillProgress_index-0"]')

    cy.cdVisit('/subjects/subj2')
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '1')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '2')
    cy.get('[data-cy="skillProgress_index-0"]')
  })

})