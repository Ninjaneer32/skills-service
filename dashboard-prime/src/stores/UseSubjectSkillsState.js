import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRoute } from 'vue-router'
import SkillsService from '@/components/skills/SkillsService.js'

export const useSubjectSkillsState = defineStore('subjectSkillsState', () => {
  const subjectSkills = ref([])
  const groupSkills = ref(new Map())
  const loadingSubjectSkills = ref(false)
  const route = useRoute()

  function setSubjectSkills(value) {
    subjectSkills.value = value
  }

  function setLoadingSubjectSkills(value) {
    loadingSubjectSkills.value = value
  }

  const hasSkills = computed(() => {
    return subjectSkills.value.length > 0
  })

  function loadSubjectSkills(projectId, subjectId, updateLoadingFlag = true) {
    if (updateLoadingFlag) {
      setLoadingSubjectSkills(true)
    }
    return new Promise((resolve, reject) => {
      SkillsService.getSubjectSkills(projectId, subjectId)
        .then((loadedSkills) => {
          const updatedSkills = loadedSkills.map((loadedSkill) => ({
            ...loadedSkill,
            subjectId: route.params.subjectId,
          }))
          setSubjectSkills(updatedSkills)
          resolve(updatedSkills)
        })
        .catch((error) => reject(error))
        .finally(() => {
          if (updateLoadingFlag) {
            setLoadingSubjectSkills(false)
          }
        })
    })
  }

  function setGroupSkills(groupId, value) {
    groupSkills.value.set(groupId, value)
  }
  function getGroupSkills(groupId) {
    return groupSkills.value.get(groupId) || []
  }

  function loadGroupSkills(projectId, groupId) {
    return SkillsService.getGroupSkills(projectId, groupId)
      .then((loadedSkills) => {
        const updatedSkills = loadedSkills.map((loadedSkill) => ({
          ...loadedSkill,
          subjectId: route.params.subjectId
        }))
        setGroupSkills(groupId, updatedSkills)
      })
  }


  return {
    subjectSkills,
    loadingSubjectSkills,
    setLoadingSubjectSkills,
    loadSubjectSkills,
    hasSkills,
    setGroupSkills,
    getGroupSkills,
    loadGroupSkills,
  }

})