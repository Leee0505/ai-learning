import request from './request'

export function getMonitorOverviewApi() {
  return request.get('/admin/monitor/overview')
}

export function getKafkaMetricsApi() {
  return request.get('/admin/monitor/kafka')
}

export function getRedisMetricsApi() {
  return request.get('/admin/monitor/redis')
}

export function getApiMetricsApi() {
  return request.get('/admin/monitor/api')
}
