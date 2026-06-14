import request from './request'

export function createTicket(data) {
  return request.post('/tickets', data)
}

export function listTickets(params) {
  return request.get('/tickets', { params })
}

export function getTicketDetail(id) {
  return request.get(`/tickets/${id}`)
}

export function updateTicket(id, data) {
  return request.put(`/tickets/${id}`, data)
}

export function deleteTicket(id) {
  return request.delete(`/tickets/${id}`)
}

export function changeTicketStatus(id, data) {
  return request.patch(`/tickets/${id}/status`, data)
}

export function assignTicket(id, data) {
  return request.patch(`/tickets/${id}/assign`, data)
}

export function addReply(id, data) {
  return request.post(`/tickets/${id}/replies`, data)
}

export function uploadAttachment(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  // Let axios/browser set Content-Type automatically — it must include the multipart boundary
  return request.post(`/tickets/${id}/attachments`, formData)
}

export function getDashboardStats() {
  return request.get('/tickets/stats')
}

export function downloadAttachment(id) {
  return request.get(`/attachments/${id}`, { responseType: 'blob' })
}

