import request from './request'

// Admin-only: list all tenants (for user management dropdowns)
export function getTenantsApi() {
  return request.get('/admin/tenants')
}

// Public: list enabled tenants (for registration form dropdown)
export function getPublicTenantsApi() {
  return request.get('/public/tenants')
}
