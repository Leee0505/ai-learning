import request from './request'

// Template CRUD (admin)
export function getTemplatesApi() { return request.get('/admin/surveys') }
export function getTemplateApi(id) { return request.get(`/admin/surveys/${id}`) }
export function createTemplateApi(data) { return request.post('/admin/surveys', data) }
export function updateTemplateApi(id, data) { return request.put(`/admin/surveys/${id}`, data) }
export function deleteTemplateApi(id) { return request.delete(`/admin/surveys/${id}`) }

// Builder (admin)
export function addPageApi(templateId, data) { return request.post(`/admin/surveys/${templateId}/pages`, data) }
export function deletePageApi(pageId) { return request.delete(`/admin/surveys/pages/${pageId}`) }
export function addSectionApi(pageId, data) { return request.post(`/admin/surveys/pages/${pageId}/sections`, data) }
export function deleteSectionApi(sectionId) { return request.delete(`/admin/surveys/sections/${sectionId}`) }
export function addQuestionApi(sectionId, data) { return request.post(`/admin/surveys/sections/${sectionId}/questions`, data) }
export function updateQuestionApi(questionId, data) { return request.put(`/admin/surveys/questions/${questionId}`, data) }
export function deleteQuestionApi(questionId) { return request.delete(`/admin/surveys/questions/${questionId}`) }
export function addRuleApi(templateId, data) { return request.post(`/admin/surveys/${templateId}/rules`, data) }
export function deleteRuleApi(ruleId) { return request.delete(`/admin/surveys/rules/${ruleId}`) }

// Instances (admin)
export function createInstanceApi(data) { return request.post('/admin/surveys/instances', data) }
export function getTemplateInstancesApi(templateId) { return request.get(`/admin/surveys/instances?templateId=${templateId}`) }

// Fill (user)
export function getMyInstancesApi() { return request.get('/surveys/instances') }
export function getFillDataApi(instanceId) { return request.get(`/surveys/instances/${instanceId}/fill`) }
export function saveAnswerApi(instanceId, data) { return request.put(`/surveys/instances/${instanceId}/answers`, data) }
export function submitSurveyApi(instanceId, data) { return request.post(`/surveys/instances/${instanceId}/submit`, data) }
