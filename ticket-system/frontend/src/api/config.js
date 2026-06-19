import request from './request'

export function getFieldsApi() {
  return request.get('/admin/config/fields')
}

export function createFieldApi(data) {
  return request.post('/admin/config/fields', data)
}

export function updateFieldApi(id, data) {
  return request.put(`/admin/config/fields/${id}`, data)
}

export function deleteFieldApi(id) {
  return request.delete(`/admin/config/fields/${id}`)
}

export function reorderFieldsApi(data) {
  return request.put('/admin/config/fields/reorder', data)
}

export function getSlaApi() {
  return request.get('/admin/config/sla')
}

export function updateSlaApi(id, data) {
  return request.put(`/admin/config/sla/${id}`, data)
}
