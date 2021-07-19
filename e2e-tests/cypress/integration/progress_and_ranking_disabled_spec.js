    /*
 * Copyright 2020 SkillTree
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
describe('Project and Ranking Views are disabled Tests', () => {

    it('Home page preference is not shown', () => {
        cy.visit('/settings/preferences');
        cy.get('[data-cy="defaultHomePageSetting"]');
        cy.get('[data-cy="rankOptOut"]');

        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            })
        }).as('getConfig')

        cy.visit('/settings/preferences');
        cy.wait('@getConfig')
        cy.get('[data-cy="rankOptOut"]');
        cy.get('[data-cy="defaultHomePageSetting"]').should('not.exist');
    });

    it('Admin and Progress and Ranking navigation is NOT shown', () => {
        cy.visit('/administrator');
        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-cy="settingsButton-navToMyProgress"]').should('exist');
        cy.get('[data-cy="settingsButton-navToProjectAdmin"]').should('exist');

        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            })
        }).as('getConfig')

        cy.visit('/administrator');
        cy.wait('@getConfig')

        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-cy="settingsButton-navToMyProgress"]').should('not.exist');
        cy.get('[data-cy="settingsButton-navToProjectAdmin"]').should('not.exist');
    });

    it('Project level enable prod-mode setting must NOT be shown', () => {
        cy.createProject(1);

        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[ data-cy="productionModeSetting"]').should('exist');
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    rankingAndProgressViewsEnabled: 'false',
                },
            })
        }).as('getConfig')

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getConfig')

        cy.get('[ data-cy="productionModeSetting"]').should('not.exist');
    });

});

