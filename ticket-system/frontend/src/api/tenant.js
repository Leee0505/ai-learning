import request from './request'

export function getTenantsApi() {
  return request.get('/admin/tenants')
}
