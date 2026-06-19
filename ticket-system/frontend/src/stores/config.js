import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getFieldsApi, createFieldApi, updateFieldApi,
  deleteFieldApi, reorderFieldsApi,
  getSlaApi, updateSlaApi
} from '@/api/config'

export const useConfigStore = defineStore('config', () => {
  const fields = ref([])
  const slaRules = ref([])
  const loading = ref(false)

  // ── Field Actions ──

  async function fetchFields() {
    loading.value = true
    try {
      const { data } = await getFieldsApi()
      if (data?.code === 200) {
        fields.value = data.data || []
      }
    } catch (e) {
      console.error('[config store] fetchFields:', e)
    } finally {
      loading.value = false
    }
  }

  async function createField(fieldData) {
    const { data } = await createFieldApi(fieldData)
    if (data.code === 200) {
      await fetchFields()
    }
    return data
  }

  async function updateField(id, fieldData) {
    const { data } = await updateFieldApi(id, fieldData)
    if (data.code === 200) {
      await fetchFields()
    }
    return data
  }

  async function deleteField(id) {
    const { data } = await deleteFieldApi(id)
    if (data.code === 200) {
      await fetchFields()
    }
    return data
  }

  async function reorderFields(items) {
    const { data } = await reorderFieldsApi({ items })
    if (data.code === 200) {
      await fetchFields()
    }
    return data
  }

  // ── SLA Actions ──

  async function fetchSla() {
    try {
      const { data } = await getSlaApi()
      if (data?.code === 200) {
        slaRules.value = data.data || []
      }
    } catch (e) {
      console.error('[config store] fetchSla:', e)
    }
  }

  async function updateSla(id, slaData) {
    const { data } = await updateSlaApi(id, slaData)
    if (data.code === 200) {
      await fetchSla()
    }
    return data
  }

  return {
    fields, slaRules, loading,
    fetchFields, createField, updateField, deleteField, reorderFields,
    fetchSla, updateSla
  }
})
