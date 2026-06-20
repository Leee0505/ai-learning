import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getTemplatesApi, getTemplateApi, createTemplateApi, deleteTemplateApi } from '@/api/survey'

export const useSurveyStore = defineStore('survey', () => {
  const templates = ref([])
  const currentTemplate = ref(null)
  const loading = ref(false)
  const page = ref(1)
  const size = ref(12)
  const total = ref(0)
  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

  async function fetchTemplates(p) {
    if (p !== undefined) page.value = p
    loading.value = true
    try {
      const { data } = await getTemplatesApi(page.value, size.value)
      if (data?.code === 200) {
        templates.value = data.data?.records || []
        total.value = data.data?.total || 0
      }
    } finally { loading.value = false }
  }

  async function fetchTemplate(id) {
    const { data } = await getTemplateApi(id)
    if (data?.code === 200) currentTemplate.value = data.data
    return data
  }

  async function createTemplate(form) {
    const { data } = await createTemplateApi(form)
    if (data.code === 200) await fetchTemplates(1)
    return data
  }

  async function deleteTemplate(id) {
    const { data } = await deleteTemplateApi(id)
    if (data.code === 200) await fetchTemplates()
    return data
  }

  return { templates, currentTemplate, loading, page, size, total, totalPages, fetchTemplates, fetchTemplate, createTemplate, deleteTemplate }
})
