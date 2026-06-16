import request from './request'

export function getUsersApi(params) {
  return request.get('/admin/users', { params })
}

export function getUserApi(id) {
  return request.get(`/admin/users/${id}`)
}

export function createUserApi(data) {
  return request.post('/admin/users', data)
}

export function updateUserApi(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

export function deleteUserApi(id) {
  return request.delete(`/admin/users/${id}`)
}

export function changeUserRoleApi(id, data) {
  return request.patch(`/admin/users/${id}/role`, data)
}

export function toggleUserStatusApi(id, data) {
  return request.patch(`/admin/users/${id}/status`, data)
}
