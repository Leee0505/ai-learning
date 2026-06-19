import request from './request'

export function getAgentsApi() {
  return request.get('/agents')
}
