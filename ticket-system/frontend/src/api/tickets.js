import request from './request'

export function createTicket(data) {
  return request.post('/tickets', data)
}

export function listTickets(params) {
  return request.get('/tickets', { params: { sortOrder: 'desc', ...params } })
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

export function deleteBatchTickets(ids) {
  return request.delete('/tickets/batch', { data: ids })
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

export function editReply(ticketId, replyId, data) {
  return request.put(`/tickets/${ticketId}/replies/${replyId}`, data)
}

export function deleteReply(ticketId, replyId) {
  return request.delete(`/tickets/${ticketId}/replies/${replyId}`)
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

export function getAgentStats() {
  return request.get('/tickets/stats/agent')
}

export function downloadAttachment(id) {
  return request.get(`/attachments/${id}`, { responseType: 'blob' })
}

