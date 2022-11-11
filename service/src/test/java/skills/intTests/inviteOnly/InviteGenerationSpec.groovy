/**
 * Copyright 2022 SkillTree
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
package skills.intTests.inviteOnly

import com.google.common.collect.Sets
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.repos.UserRoleRepo
import skills.utils.WaitFor
import spock.lang.IgnoreRest

class InviteGenerationSpec extends InviteOnlyBaseSpec {

    @Autowired
    UserRoleRepo userRoleRepo

    def "Can generate more than one invite at once"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo", "numbertwo@email.bar", "numberone@email.baz"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 2 }

        Set<String> inviteCodes = Sets.newHashSet()

        then:
        greenMail.getReceivedMessages().find { it.getAllRecipients()[0].toString() == "someemail@email.foo" }
        greenMail.getReceivedMessages().find { it.getAllRecipients()[0].toString() == "numbertwo@email.bar" }
        greenMail.getReceivedMessages().find{ it.getAllRecipients()[0].toString() == "numberone@email.baz"}
        greenMail.getReceivedMessages().each {
            String inviteCode = extractInviteFromEmail(it.content.toString())
            assert !inviteCodes.contains(inviteCode), "non-unique invite code was generated"
            inviteCodes.add(inviteCode)
        }
    }

    def "Invalid recipient addresses are rejected, valid recipients are sent"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        when:

        def result = skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo", "numbertwo", "@email.baz"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()

        then:
        email.length == 1
        email[0].getAllRecipients()[0].toString() == "someemail@email.foo"
        result.projectId == proj.projectId
        result.successful.size() == 1
        result.successful[0] == "someemail@email.foo"
        result.unsuccessful.size() == 2
        result.unsuccessful.sort() == ["numbertwo is not a valid email", "@email.baz is not a valid email"].sort()
    }

    def "invite expiration is configurable"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        when:

        def user = getRandomUsers(1, true)[0]
        def userService = createService(user)

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT1S", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()
        String expiredCode = extractInviteFromEmail(email[0].content.toString())
        Thread.currentThread().sleep(1200)
        userService.joinProject(proj.projectId, expiredCode)

        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.BAD_REQUEST
        err.getMessage().contains("Invitation Code has expired")
        err.getMessage().contains("errorCode:ExpiredInvitationCode")
    }

    def "invite can only be used once"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)
        def userService = createService(users[0])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()
        String inviteCode = extractInviteFromEmail(email[0].content.toString())
        userService.joinProject(proj.projectId, inviteCode)

        when:
        def user2Service = createService(users[1])
        user2Service.joinProject(proj.projectId, inviteCode)

        then:
        def err = thrown(SkillsClientException)
        err.message.contains("explanation:Invitation Code has already been used, errorCode:ClaimedInvitationCode")
    }

    def "invalid invite cannot be used"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)
        def userService = createService(users[0])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()
        String inviteCode = extractInviteFromEmail(email[0].content.toString())
        userService.joinProject(proj.projectId, inviteCode)

        when:
        def user2Service = createService(users[1])
        user2Service.joinProject(proj.projectId, inviteCode)

        then:
        def err = thrown(SkillsClientException)
        err.message.contains("explanation:Invitation Code has already been used, errorCode:ClaimedInvitationCode")
    }

    def "cannot use invite for a different project"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        def proj2 = SkillsFactory.createProject(101)

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.changeSetting(proj2.projectId, "invite_only", [projectId: proj2.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)
        def userService = createService(users[0])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()
        String inviteCode = extractInviteFromEmail(email[0].content.toString())

        when:
        userService.joinProject(proj2.projectId, inviteCode)

        then:
        def err = thrown(SkillsClientException)
        err.message.contains("explanation:Invitation Code does not exist for Project, errorCode:InvalidInvitationCode")
    }

    def "invites cannot be sent if email is not configured"() {
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "",
                "port"       : -1,
                "protocol"   : "",
                "authEnabled": "",
                "tlsEnabled" : ""
        ])
        rootSkillsService.addOrUpdateGlobalSetting("public_url",
                ["setting": "public_url", "value": ''])
        rootSkillsService.addOrUpdateGlobalSetting("from_email",
                ["setting": "from_email", "value": ''])

        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])

        then:
        def err = thrown(SkillsClientException)
        err.message.contains('explanation:Project Invites can only be used if email has been configured for this instance')

    }

}
