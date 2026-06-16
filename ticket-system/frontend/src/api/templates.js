import request from './request'

export function listTemplates(category) {
  return request.get('/templates', { params: { category } })
}

export function createTemplate(data) {
  return request.post('/templates', data)
}

export function updateTemplate(id, data) {
  return request.put(`/templates/${id}`, data)
}

export function deleteTemplate(id) {
  return request.delete(`/templates/${id}`)
}
