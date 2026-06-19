import request from './request'

export function getNotificationsApi(params) {
  return request.get('/notifications', { params })
}

export function getUnreadCountApi() {
  return request.get('/notifications/unread-count')
}

export function markReadApi(id) {
  return request.put(`/notifications/${id}/read`)
}

export function markAllReadApi() {
  return request.put('/notifications/read-all')
}
