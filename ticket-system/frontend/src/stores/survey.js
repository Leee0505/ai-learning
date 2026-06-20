import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTemplatesApi, getTemplateApi, createTemplateApi, deleteTemplateApi } from '@/api/survey'

export const useSurveyStore = defineStore('survey', () => {
  const templates = ref([])
  const currentTemplate = ref(null)
  const loading = ref(false)

  async function fetchTemplates() {
    loading.value = true
    try {
      const { data } = await getTemplatesApi()
      if (data?.code === 200) templates.value = data.data || []
    } finally { loading.value = false }
  }

  async function fetchTemplate(id) {
    const { data } = await getTemplateApi(id)
    if (data?.code === 200) currentTemplate.value = data.data
    return data
  }

  async function createTemplate(form) {
    const { data } = await createTemplateApi(form)
    if (data.code === 200) await fetchTemplates()
    return data
  }

  async function deleteTemplate(id) {
    const { data } = await deleteTemplateApi(id)
    if (data.code === 200) await fetchTemplates()
    return data
  }

  return { templates, currentTemplate, loading, fetchTemplates, fetchTemplate, createTemplate, deleteTemplate }
})
