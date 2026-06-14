import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import {
  listTickets, getTicketDetail, createTicket, updateTicket, deleteTicket,
  changeTicketStatus, assignTicket, addReply, uploadAttachment
} from '@/api/tickets'

export const useTicketStore = defineStore('tickets', () => {
  // ── State ──
  const tickets = ref([])
  const currentTicket = ref(null)
  const loading = ref(false)
  const total = ref(0)
  const page = ref(1)
  const size = ref(20)

  const filters = reactive({
    status: '',
    priority: '',
    category: '',
    keyword: ''
  })

  // ── Actions ──

  async function fetchTickets() {
    loading.value = true
    try {
      const { data } = await listTickets({
        page: page.value,
        size: size.value,
        status: filters.status || undefined,
        priority: filters.priority || undefined,
        category: filters.category || undefined,
        keyword: filters.keyword || undefined
      })
      if (data.code === 200) {
        tickets.value = data.data.records
        total.value = data.data.total
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchTicketDetail(id) {
    loading.value = true
    try {
      const { data } = await getTicketDetail(id)
      if (data.code === 200) {
        currentTicket.value = data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createNewTicket(formData) {
    return await createTicket(formData)
  }

  async function updateExistingTicket(id, formData) {
    const { data } = await updateTicket(id, formData)
    if (data.code === 200) {
      currentTicket.value = data.data
    }
    return data
  }

  async function removeTicket(id) {
    await deleteTicket(id)
  }

  async function changeTicketStatusAction(id, status) {
    const { data } = await changeTicketStatus(id, { status })
    if (data.code === 200) {
      currentTicket.value = data.data
    }
    return data
  }

  async function assignTicketAction(id, assignedTo) {
    const { data } = await assignTicket(id, { assignedTo })
    if (data.code === 200) {
      currentTicket.value = data.data
    }
    return data
  }

  async function addReplyAction(ticketId, content, isInternal) {
    const { data } = await addReply(ticketId, { content, isInternal })
    return data
  }

  async function uploadFile(ticketId, file) {
    return await uploadAttachment(ticketId, file)
  }

  function setFilters(newFilters) {
    Object.assign(filters, newFilters)
    page.value = 1
    fetchTickets()
  }

  function resetFilters() {
    filters.status = ''
    filters.priority = ''
    filters.category = ''
    filters.keyword = ''
    page.value = 1
    fetchTickets()
  }

  function setPage(newPage) {
    page.value = newPage
    fetchTickets()
  }

  function setSize(newSize) {
    size.value = newSize
    page.value = 1
    fetchTickets()
  }

  return {
    tickets, currentTicket, loading, total, page, size, filters,
    fetchTickets, fetchTicketDetail,
    createNewTicket, updateExistingTicket, removeTicket,
    changeTicketStatusAction, assignTicketAction,
    addReplyAction, uploadFile,
    setFilters, resetFilters, setPage, setSize
  }
})