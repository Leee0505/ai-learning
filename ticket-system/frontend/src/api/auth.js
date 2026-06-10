import request from './request'

export function loginApi(data) {
  return request.post('/auth/login', data)
}

export function registerApi(data) {
  return request.post('/auth/register', data)
}

export function logoutApi() {
  return request.post('/auth/logout')
}

export function refreshApi(data) {
  return request.post('/auth/refresh', data)
}

export function getCurrentUserApi() {
  return request.get('/auth/me')
}

export function inviteApi(data) {
  return request.post('/auth/invite', data)
}

export function acceptInviteApi(data) {
  return request.post('/auth/accept-invite', data)
}
