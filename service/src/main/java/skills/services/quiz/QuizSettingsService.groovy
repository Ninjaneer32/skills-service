/**
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
package skills.services.quiz

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.math.NumberUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillQuizException
import skills.controller.request.model.QuizSettingsRequest
import skills.controller.result.model.QuizSettingsRes
import skills.controller.result.model.SettingsResult
import skills.quizLoading.QuizSettings
import skills.services.settings.Settings
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.QuizSetting
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.QuizSettingsRepo

@Service
@Slf4j
class QuizSettingsService {

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Transactional
    void copySettings(String fromQuizId, String toQuizId) {
        List<QuizSettingsRes> fromSettings = getSettings(fromQuizId)
        List<QuizSettingsRequest> toSettings = new ArrayList<QuizSettingsRequest>()

        fromSettings.forEach( setting -> {
            QuizSettingsRequest request = new QuizSettingsRequest(value: setting.value, setting: setting.setting)
            toSettings.add(request)
        })

        saveSettings(toQuizId, toSettings)
    }

    @Transactional
    void saveSettings(String quizId, List<QuizSettingsRequest> settingsRequests) {
        Integer quizRefId = getQuizDefRefId(quizId)
        settingsRequests.each {
            validateProvidedQuizSetting(quizId, it)

            QuizSetting existing = quizSettingsRepo.findBySettingAndQuizRefId(it.setting, quizRefId)
            if (existing) {
                existing.value = it.value
                quizSettingsRepo.save(existing)
            } else {
                quizSettingsRepo.save(new QuizSetting(setting: it.setting, value: it.value, quizRefId: quizRefId))
            }

            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: DashboardAction.Create, item: DashboardItem.Settings,
                    itemId: quizId, quizId: quizId,
                    actionAttributes: [
                            setting: it.setting,
                            value: it.value,
                    ]
            ))
        }
    }

    private void validateProvidedQuizSetting(String quizId, QuizSettingsRequest quizSettingsRequest) {
        QuizValidator.isNotBlank(quizSettingsRequest.setting, "settings.setting", quizId)
        QuizValidator.isNotBlank(quizSettingsRequest.value, "settings.value", quizId)

        if (quizSettingsRequest.setting == QuizSettings.MaxNumAttempts.setting || quizSettingsRequest.setting == QuizSettings.MinNumQuestionsToPass.setting) {
            if (!NumberUtils.isCreatable(quizSettingsRequest.value)) {
                throw new SkillQuizException("Provided value [${quizSettingsRequest.value}] for [${quizSettingsRequest.setting}] setting must be numeric", quizId, ErrorCode.BadParam)
            }

            Integer maxNumAttempts = Integer.valueOf(quizSettingsRequest.value)
            if (maxNumAttempts < -1) {
                throw new SkillQuizException("Provided value [${quizSettingsRequest.value}] for [${quizSettingsRequest.setting}] setting must be >= -1", quizId, ErrorCode.BadParam)
            }
        }
        if (quizSettingsRequest.setting == QuizSettings.MinNumQuestionsToPass.setting) {
            Integer minNumQuestionsToPass = Integer.valueOf(quizSettingsRequest.value)
            int numDeclaredQuestions = quizQuestionDefRepo.countByQuizId(quizId)
            if (numDeclaredQuestions == 0) {
                throw new SkillQuizException("Cannot modify [${quizSettingsRequest.setting}] becuase there is 0 declared questions", quizId, ErrorCode.BadParam)
            }

            if (numDeclaredQuestions < minNumQuestionsToPass) {
                throw new SkillQuizException("Provided [${quizSettingsRequest.setting}] setting [${minNumQuestionsToPass}] must be less than [${numDeclaredQuestions}] declared questions.", quizId, ErrorCode.BadParam)
            }
        }

    }

    @Transactional(readOnly = true)
    List<QuizSettingsRes> getSettings(String quizId) {
        Integer quizRefId = getQuizDefRefId(quizId)
        List<QuizSetting> quizSettings = quizSettingsRepo.findAllByQuizRefId(quizRefId)
        List<QuizSettingsRes> res = quizSettings.collect {
            new QuizSettingsRes(setting: it.setting, value: it.value, created: it.created, updated: it.updated)
        } ?: []

        UserInfo currentUser = userInfoService.getCurrentUser()
        List<String> usrRoles = currentUser.authorities.collect { it.authority.toUpperCase() }
        if (usrRoles.contains(RoleName.ROLE_QUIZ_ADMIN.toString())) {
            res.add(new QuizSettingsRes(setting: QuizSettings.QuizUserRole.setting, value: RoleName.ROLE_QUIZ_ADMIN.toString()))
        } else if (usrRoles.contains(RoleName.ROLE_QUIZ_READ_ONLY.toString())) {
            res.add(new QuizSettingsRes(setting: QuizSettings.QuizUserRole.setting, value: RoleName.ROLE_QUIZ_READ_ONLY.toString()))
        }

        return res.sort({ it.setting })
    }

    @Profile
    private Integer getQuizDefRefId(String quizId) {
        Integer id = quizDefRepo.getQuizRefIdByQuizIdIgnoreCase(quizId)
        if (id == null) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return id
    }
}
