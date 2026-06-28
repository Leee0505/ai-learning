<template>
  <div class="fill-page">
    <!-- Loading skeleton -->
    <div v-if="loading" class="fill-loading">
      <div class="skeleton-list">
        <div v-for="i in 3" :key="i" class="skeleton-card">
          <div class="skeleton-line skeleton-line--med"></div>
          <div class="skeleton-line skeleton-line--long"></div>
        </div>
      </div>
    </div>

    <!-- Instance list (when no instance selected) -->
    <div v-else-if="!currentInstance" class="fill-instances">
      <h1 class="fill-instances-title">My Surveys</h1>
      <div v-if="instances.length === 0" class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon" aria-hidden="true"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
        <p class="empty-title">No pending surveys</p>
        <p class="empty-desc">You don't have any surveys assigned yet. Check back later or contact your admin.</p>
      </div>
      <div v-else class="instance-list">
        <div v-for="inst in instances" :key="inst.id" class="instance-card" @click="openSurvey(inst.id)">
          <h3>{{ inst.title }}</h3>
          <div class="instance-meta">
            <span :class="['status-badge', 'status-' + (inst.status || '').toLowerCase()]">{{ (inst.status || '').replace(/_/g, ' ') }}</span>
            <span>{{ inst.completedPages }}/{{ inst.totalPages }} pages</span>
            <span v-if="inst.assignedToName" class="instance-assignee" :title="'Assigned to'">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              {{ inst.assignedToName }}
            </span>
            <span v-if="inst.templateTitle" class="instance-template">{{ inst.templateTitle }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Fill view -->
    <div v-else class="fill-layout">
      <!-- Top bar — back only -->
      <div class="fill-topbar">
        <button class="fill-back" @click="currentInstance = null" aria-label="Back to list">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
          My Surveys
        </button>
      </div>

      <div v-if="!fillData" class="fill-empty">Loading survey data...</div>
      <div v-else-if="!fillData.pages?.length" class="fill-empty">No pages found in this survey.</div>
      <div v-else class="fill-body">
        <!-- Survey Head card -->
        <div class="survey-head">
          <h2 class="survey-head-title">{{ currentInstance.title }}</h2>
          <div class="survey-head-cols">
            <!-- Left: survey-level info -->
            <div class="survey-head-col">
              <div class="survey-head-item">
                <span class="survey-head-label">Survey</span>
                <span :class="['status-badge', 'status-' + (fillData?.instanceStatus || '').toLowerCase()]">{{ (fillData?.instanceStatus || '').replace(/_/g, ' ') }}</span>
              </div>
              <div class="survey-head-item">
                <span class="survey-head-label">Assigned to</span>
                <span class="survey-head-value" v-if="fillData?.assignedToName">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  {{ fillData.assignedToName }}
                </span>
                <span class="survey-head-na" v-else>&mdash;</span>
                <button v-if="fillData?.currentUsername && fillData.assignedToName === fillData.currentUsername" class="reassign-btn" @click.stop="openReassign('instance')" title="Reassign instance">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                </button>
              </div>
            </div>
            <!-- Right: page-level info -->
            <div class="survey-head-col" v-if="visiblePages.length">
              <div class="survey-head-item">
                <span class="survey-head-label">Page</span>
                <span class="survey-head-value">{{ visiblePageNumber }} of {{ visiblePages.length }}</span>
              </div>
              <div class="survey-head-item" v-if="currentPage">
                <span class="survey-head-label">Page status</span>
                <span class="survey-head-page-status" :class="'survey-head-page-status--' + pageStatusClass(currentPage.id)">
                  {{ pageStatusLabel(currentPage.id) }}
                </span>
              </div>
              <div class="survey-head-item" v-if="currentPage">
                <span class="survey-head-label">Page assigned to</span>
                <span class="survey-head-value" v-if="pageAssignee(currentPage.id)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  {{ pageAssignee(currentPage.id) }}
                </span>
                <span class="survey-head-na" v-else>&mdash;</span>
                <button v-if="fillData?.currentUsername && pageAssignee(currentPage.id) === fillData.currentUsername" class="reassign-btn" @click.stop="openReassign('page', currentPage.id)" title="Reassign page">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                </button>
              </div>
            </div>
          </div>
          <div class="survey-head-progress" v-if="visiblePages.length">
            <div class="survey-head-progress-bar">
              <div class="survey-head-progress-fill" :style="{ width: (visiblePageNumber / visiblePages.length * 100) + '%' }"></div>
            </div>
          </div>
          <!-- Actions dropdown (reopen / complete instance) -->
          <div class="survey-head-actions" v-if="hasHeadActions">
            <el-dropdown trigger="click" @command="handleHeadAction">
              <button class="head-action-btn" aria-label="Survey actions" title="Actions">
                <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/></svg>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="isCurrentPageCompleted" command="reopen-page">Reopen this Page</el-dropdown-item>
                  <el-dropdown-item v-if="canCompleteInstance" command="complete-instance">Complete Instance</el-dropdown-item>
                  <el-dropdown-item v-if="canReopenInstance" command="reopen-instance">Reopen entire Instance</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- Content card — wraps tabs + questions for consistent alignment -->
        <div class="fill-content-card">
          <!-- Page tabs (clean — status shown as colored ring on number) -->
          <div class="fill-page-tabs">
            <button v-for="(page, pi) in fillData.pages" :key="'fp'+page.id"
                    v-show="!isHidden(page)"
                    class="fill-tab" :class="{ 'fill-tab--active': currentPageIdx === pi }"
                    @click="switchPage(pi)"
                    :title="page.title + ' — ' + pageStatusLabel(page.id)">
              <span class="fill-tab-num" :class="'fill-tab-num--' + pageStatusClass(page.id)">{{ pi + 1 }}</span>
              {{ page.title }}
            </button>
          </div>

          <!-- Section tabs -->
          <div v-if="currentPage?.sections?.length" class="fill-section-tabs">
            <button v-for="(section, si) in currentPage.sections" :key="'fs'+section.id"
                    class="fill-section-tab" :class="{ 'fill-section-tab--active': currentSectionIdx === si }"
                    @click="currentSectionIdx = si">
              <span class="fill-section-tab-num">{{ si + 1 }}</span>
              {{ section.title }}
            </button>
          </div>

          <!-- Questions -->
          <main class="fill-content">
            <div v-if="!currentSection" class="fill-empty">No sections on this page.</div>
            <div v-else>
              <Transition name="fade" mode="out-in">
                <div :key="currentSection.id">
                  <div v-for="q in visibleQuestions(currentSection)" :key="q.id" class="fill-question">
                <label :for="'q'+q.id" class="fill-q-label">
                  {{ q.title }}
                  <span v-if="q.required" class="fill-q-required">*</span>
                </label>

              <!-- TEXT (debounced save on input, flush on blur) -->
              <input v-if="q.type === 'TEXT'" :id="'q'+q.id" type="text" class="input"
                     :disabled="!canEditCurrentPage"
                     :value="answers[q.id] || ''"
                     @input="setAnswer(q.id, $event.target.value)"
                     @blur="flushPendingSaves()" />

              <!-- TEXTAREA (debounced save on input, flush on blur) -->
              <textarea v-else-if="q.type === 'TEXTAREA'" :id="'q'+q.id" class="input textarea" rows="3"
                        :disabled="!canEditCurrentPage"
                        :value="answers[q.id] || ''"
                        @input="setAnswer(q.id, $event.target.value)"
                        @blur="flushPendingSaves()"></textarea>

              <!-- DATE (debounced save on input, flush on blur) -->
              <input v-else-if="q.type === 'DATE'" :id="'q'+q.id" type="text" class="input"
                     :disabled="!canEditCurrentPage"
                     :value="answers[q.id] || ''" placeholder="YYYY-MM-DD"
                     @input="setAnswer(q.id, $event.target.value)"
                     @blur="flushPendingSaves()" />

              <!-- SINGLE_CHOICE / DROPDOWN (click to select, click again to clear) -->
              <div v-else-if="q.type === 'SINGLE_CHOICE' || q.type === 'DROPDOWN'" class="fill-options" :class="{ 'fill-options--disabled': !canEditCurrentPage }">
                <label v-for="(opt, oi) in parseOptions(q.options)" :key="oi" class="fill-opt"
                       @click.prevent="canEditCurrentPage && handleSingleChoice(q.id, opt.key)">
                  <span class="opt-dot" :class="{ 'opt-dot--checked': answers[q.id] === opt.key }"></span>
                  {{ opt.label }}
                </label>
              </div>

              <!-- MULTI_CHOICE (save immediately) -->
              <div v-else-if="q.type === 'MULTI_CHOICE'" class="fill-options" :class="{ 'fill-options--disabled': !canEditCurrentPage }">
                <label v-for="(opt, oi) in parseOptions(q.options)" :key="oi" class="fill-opt">
                  <input type="checkbox" :value="opt.key" :disabled="!canEditCurrentPage"
                         :checked="multiChecked(q.id, opt.key)" @change="toggleMulti(q.id, opt.key)" />
                  {{ opt.label }}
                </label>
              </div>

              <!-- RATING (click star to rate, click same star again to clear) -->
              <div v-else-if="q.type === 'RATING'" class="fill-rating" :class="{ 'fill-rating--disabled': !canEditCurrentPage }">
                <button v-for="i in getRatingMax(q.options)" :key="i" class="fill-rating-btn"
                        :class="{ 'fill-rating-btn--active': Number(answers[q.id] || 0) >= i }"
                        :disabled="!canEditCurrentPage"
                        @click="handleRating(q.id, i)" :aria-label="'Rate ' + i">
                  <svg viewBox="0 0 24 24" :fill="Number(answers[q.id] || 0) >= i ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.5" width="28" height="28"><polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/></svg>
                </button>
              </div>

              <!-- CASCADER / TABLE -->
              <div v-else class="input muted">{{ q.type.replace('_',' ') }} — not yet supported</div>
            </div>
          </div>
          </Transition>
          </div>

          <!-- Navigation -->
          <div class="fill-nav-btns">
            <button :disabled="currentPageIdx === 0" class="btn-secondary" @click="prevPage">Previous</button>
            <div class="fill-nav-center">
              <button v-if="!isCurrentPageCompleted" class="btn-primary" @click="handleCompletePage" :disabled="completingPage">
                {{ completingPage ? 'Completing...' : 'Complete Page' }}
              </button>
            </div>
            <button v-if="currentPageIdx < (fillData?.pages?.length || 1) - 1" class="btn-secondary" @click="nextPage">Next</button>
            <span v-else style="visibility:hidden" class="btn-secondary">Next</span>
          </div>
        </main>
        </div><!-- .fill-content-card -->

        <!-- Activity Timeline -->
        <div class="activity-section" v-if="activityLog.length">
          <button class="activity-toggle" @click="showActivity = !showActivity" :aria-expanded="showActivity" aria-label="Toggle activity log">
            <span class="activity-toggle-label">Activity ({{ activityLog.length }})</span>
            <svg class="activity-toggle-arrow" :class="{ 'activity-toggle-arrow--open': showActivity }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><polyline points="6,9 12,15 18,9"/></svg>
          </button>
          <div v-if="showActivity" class="activity-timeline">
            <div v-for="entry in activityLog" :key="entry.id" class="activity-entry" :class="'activity-entry--' + actionClass(entry.action)">
              <span class="activity-dot" :class="'activity-dot--' + actionClass(entry.action)"></span>
              <div class="activity-body">
                <div class="activity-header">
                  <span class="activity-action">{{ formatAction(entry.action) }}</span>
                  <span class="activity-time">{{ formatTime(entry.createdDate) }}</span>
                </div>
                <div v-if="entry.detail" class="activity-detail">{{ entry.detail }}</div>
              </div>
            </div>
          </div>
        </div>
      </div><!-- .fill-body -->
    </div><!-- .fill-layout -->

  </div><!-- .fill-page -->

  <!-- Reassign dialog -->
  <el-dialog v-model="reassignPopover.show"
             :title="'Reassign ' + (reassignPopover.type === 'instance' ? 'Survey' : 'Page')"
             width="380px" :close-on-click-modal="true" destroy-on-close>
	    <div v-if="reassignUsers.loading" class="reassign-loading">Loading users...</div>
	    <div v-else-if="!reassignUsers.list.length" class="reassign-empty">
	      <span class="reassign-empty-icon">👤</span>
	      <span>No users available</span>
	    </div>
	    <div v-else class="reassign-user-list">
	      <template v-for="(group, gk) in groupedReassignUsers" :key="gk">
	        <div class="reassign-user-group-label">{{ group.label }}</div>
	        <button v-for="u in group.users" :key="u.id" class="reassign-user-item"
	                @click="doReassign(u.id)">
	          <span class="reassign-user-avatar">{{ u.username.charAt(0).toUpperCase() }}</span>
	          <div class="reassign-user-info">
	            <span class="reassign-user-name">{{ u.username }}</span>
	            <span class="reassign-user-role">{{ formatRoleName(u.role) }}</span>
	          </div>
	        </button>
	      </template>
	    </div>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyInstancesApi, getFillDataApi, saveAnswerApi, submitSurveyApi, completePageApi, reopenPageApi, reopenInstanceApi, reassignInstanceApi, reassignPageApi, listUsersApi, getInstanceLogApi } from '@/api/survey'

const loading = ref(true)
const submitting = ref(false)
const completingPage = ref(false)
const activityLog = ref([])
const showActivity = ref(false)

function formatAction(action) {
  const map = { INSTANCE_CREATED: 'Instance created', ANSWER_SAVED: 'Answer saved', PAGE_COMPLETED: 'Page completed', PAGE_REOPENED: 'Page reopened', INSTANCE_REOPENED: 'Instance reopened', INSTANCE_SUBMITTED: 'Instance submitted', INSTANCE_REASSIGNED: 'Instance reassigned' }
  return map[action] || action
}
function actionClass(action) {
  if (action === 'PAGE_COMPLETED' || action === 'INSTANCE_SUBMITTED') return 'success'
  if (action.includes('REOPEN')) return 'warning'
  if (action === 'ANSWER_SAVED') return 'info'
  return 'default'
}
function formatTime(ts) { return new Date(ts).toLocaleString() }

const isLastPage = computed(() => currentPageIdx.value >= (fillData.value?.pages?.length || 1) - 1)

const isCurrentPageCompleted = computed(() => {
  if (!fillData.value || !currentPage.value) return false
  const status = fillData.value.pageStatuses?.[currentPage.value.id]
  return status === 'COMPLETED'
})

const canCompleteInstance = computed(() => {
  return fillData.value?.instanceStatus === 'SUBMITTED'
})

const canReopenInstance = computed(() => {
  const s = fillData.value?.instanceStatus
  return s === 'SUBMITTED' || s === 'COMPLETED'
})

const hasHeadActions = computed(() => {
  return isCurrentPageCompleted.value || canCompleteInstance.value || canReopenInstance.value
})
const instances = ref([])
const currentInstance = ref(null)
const fillData = ref(null)
const currentPageIdx = ref(0)
const currentSectionIdx = ref(0)
const answers = reactive({})

// Reassign
const reassignPopover = reactive({ show: false, type: 'instance', pageId: null })
const reassignUsers = reactive({ list: [], loading: false })

async function openReassign(type, pageId = null) {
  reassignPopover.type = type
  reassignPopover.pageId = pageId
  reassignPopover.show = true
  // Load users while popover shows loading state
  reassignUsers.loading = true
  reassignUsers.list = []
  try {
    const { data } = await listUsersApi({ size: 200 })
    if (data.code === 200) {
      reassignUsers.list = data.data?.records || data.data || []
    }
    if (reassignUsers.list.length === 0) {
      reassignPopover.show = false
      ElMessage.warning('No users available to reassign.')
    }
  } catch (e) {
    reassignPopover.show = false
    ElMessage.error('Failed to load users.')
  } finally {
    reassignUsers.loading = false
  }
}

const ROLE_ORDER = { ROLE_ADMIN: 0, ROLE_AGENT: 1, ROLE_USER: 2 }
const ROLE_LABELS = { ROLE_ADMIN: 'Administrators', ROLE_AGENT: 'Agents', ROLE_USER: 'Users' }

const groupedReassignUsers = computed(() => {
  // Determine current assignee name to exclude
  const currentName = reassignPopover.type === 'instance'
    ? fillData.value?.assignedToName
    : pageAssignee(reassignPopover.pageId)
  const currentUser = fillData.value?.currentUsername
  const groups = {}
  for (const u of reassignUsers.list) {
    // Skip current assignee (no-op reassign) and current user (can't assign to self)
    if (u.username === currentName || u.username === currentUser) continue
    const role = u.role || 'ROLE_USER'
    if (!groups[role]) groups[role] = { label: ROLE_LABELS[role] || role, users: [] }
    groups[role].users.push(u)
  }
  return Object.entries(groups)
    .sort(([a], [b]) => (ROLE_ORDER[a] ?? 9) - (ROLE_ORDER[b] ?? 9))
    .map(([role, g]) => ({ role, ...g }))
})

function formatRoleName(role) {
  return (role || '').replace('ROLE_', '').replace('_', ' ')
}

async function doReassign(userId) {
  // Flush any pending saves before reassigning (otherwise they'd fire against wrong instance)
  await flushPendingSaves()
  try {
    if (reassignPopover.type === 'instance') {
      const res = await reassignInstanceApi(currentInstance.value.id, { userId })
      if (res.data.code !== 200) {
        ElMessage.error(res.data.message || 'Reassign failed')
        return
      }
      // Instance reassign: current user is no longer the assignee — go back to list immediately.
      // Don't try refreshFillData() — getFillData would return ACCESS_DENIED since instance.assignee changed.
      reassignPopover.show = false
      ElMessage.success('Reassigned — returning to list')
      currentInstance.value = null
      return
    }

    // Page reassign
    const res = await reassignPageApi(currentInstance.value.id, reassignPopover.pageId, { userId })
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || 'Reassign failed')
      return
    }
    reassignPopover.show = false
    // Refresh fill data to update pageAssignees in UI
    try {
      await refreshFillData()
    } catch {
      // refresh failed (e.g., user lost all access) — go back to list
      ElMessage.success('Page reassigned — returning to list')
      currentInstance.value = null
      return
    }
    ElMessage.success('Page reassigned')
  } catch (e) {
    const msg = e.response?.data?.message || e.response?.statusText || e.message || 'Unknown error'
    ElMessage.error('Reassign failed: ' + msg)
    console.error('[Fill] Reassign error:', e)
  }
}

const currentPage = computed(() => fillData.value?.pages?.[currentPageIdx.value])
const currentSection = computed(() => currentPage.value?.sections?.[currentSectionIdx.value])
// Page-editable check: page assignee first, then fall back to instance assignee
const canEditCurrentPage = computed(() => {
  if (!fillData.value || !currentPage.value) return false
  // Completed pages are read-only (use Reopen to edit again)
  if (isCurrentPageCompleted.value) return false
  // Instance-level complete also blocks editing
  if (fillData.value.instanceStatus === 'COMPLETED') return false
  const pAssign = pageAssignee(currentPage.value.id)
  if (pAssign !== null && pAssign !== undefined) {
    return pAssign === fillData.value.currentUsername
  }
  return fillData.value.assignedToName === fillData.value.currentUsername
})
const visiblePages = computed(() => {
  const ht = fillData.value?.hiddenTargets || []
  return (fillData.value?.pages || []).filter(p => !ht.includes('PAGE:' + p.id))
})
const visiblePageNumber = computed(() => {
  const ht = fillData.value?.hiddenTargets || []
  const pages = fillData.value?.pages || []
  let count = 0
  for (let i = 0; i <= currentPageIdx.value; i++) {
    if (!ht.includes('PAGE:' + pages[i]?.id)) count++
  }
  return count
})

function isHidden(page) {
  if (!fillData.value?.hiddenTargets || !page) return false
  return fillData.value.hiddenTargets.includes('PAGE:' + page.id)
}

function pageStatusClass(pageId) {
  const s = (fillData.value?.pageStatuses || {})[pageId] || 'READY_TO_START'
  return s.toLowerCase()
}

function pageStatusLabel(pageId) {
  const s = (fillData.value?.pageStatuses || {})[pageId] || 'READY_TO_START'
  return s.replace(/_/g, ' ')
}

function pageAssignee(pageId) {
  return (fillData.value?.pageAssignees || {})[pageId] || null
}

function visibleQuestions(section) {
  const ht = fillData.value?.hiddenTargets || []
  return (section.questions || []).filter(q => !ht.includes('QUESTION:' + q.id))
}

function parseOptions(optionsJson) {
  try {
    const o = JSON.parse(optionsJson || '{}')
    const raw = o.options || []
    if (raw.length === 0) return []
    // New format: [{key, label}]
    if (typeof raw[0] === 'object') return raw
    // Old format: ["A","B","C"] → convert to [{key:"A",label:"A"}]
    return raw.map(s => ({ key: s, label: s }))
  } catch { return [] }
}
function getRatingMax(optionsJson) {
  try { return JSON.parse(optionsJson || '{}').max || 5 }
  catch { return 5 }
}
function multiChecked(qid, opt) {
  const vals = answers[qid] ? answers[qid].split(',') : []
  return vals.includes(opt)
}
function toggleMulti(qid, opt) {
  const vals = answers[qid] ? answers[qid].split(',').filter(Boolean) : []
  const idx = vals.indexOf(opt)
  if (idx >= 0) vals.splice(idx, 1)
  else vals.push(opt)
  setAnswer(qid, vals.join(','), { immediate: true })
}

/** Single-choice / Dropdown: click to select, click again to clear */
function handleSingleChoice(qid, key) {
  if (answers[qid] === key) {
    // Already selected → clear
    setAnswer(qid, '', { immediate: true })
  } else {
    setAnswer(qid, key, { immediate: true })
  }
}

/** Rating: click star to rate, click same star again to clear */
function handleRating(qid, star) {
  const current = Number(answers[qid] || 0)
  if (current === star) {
    // Clicking the same star → clear
    setAnswer(qid, '', { immediate: true })
  } else {
    setAnswer(qid, String(star), { immediate: true })
  }
}

/** Save answer: debounced for text inputs, immediate for selections */
function setAnswer(qid, value, opts = {}) {
  answers[qid] = value
  const immediate = opts.immediate || false

  if (!setAnswer._pending) setAnswer._pending = {}

  if (immediate) {
    // Cancel any pending debounced save for this question
    if (setAnswer._pending[qid]?.timer) clearTimeout(setAnswer._pending[qid].timer)
    // Save immediately and refresh visibility
    doSave(qid, value)
  } else {
    // Debounce text input — cancel previous, schedule new
    if (setAnswer._pending[qid]?.timer) clearTimeout(setAnswer._pending[qid].timer)
    setAnswer._pending[qid] = { value }
    setAnswer._pending[qid].timer = setTimeout(() => doSave(qid, value), 300)
  }
}

/** Actually send the save request + refresh visibility */
async function doSave(qid, value) {
  if (!currentInstance.value) return
  try {
    await saveAnswerApi(currentInstance.value.id, { questionId: qid, value })
    delete setAnswer._pending?.[qid]
    // Re-fetch to update visibility immediately
    await refreshFillData()
  } catch (e) {
    console.error('[Fill] Save failed for Q' + qid + ':', e)
    ElMessage.error(e.response?.data?.message || 'Failed to save answer')
  }
}

/** Flush all pending debounced saves (called before page nav / submit) */
async function flushPendingSaves() {
  if (!setAnswer._pending) return
  const pending = Object.entries(setAnswer._pending)
  for (const [qid, entry] of pending) {
    if (entry.timer) clearTimeout(entry.timer)
    await doSave(Number(qid), entry.value)
  }
}

async function refreshFillData() {
  if (!currentInstance.value) return
  try {
    const { data } = await getFillDataApi(currentInstance.value.id)
    if (data.code === 200) {
      fillData.value = data.data
      fetchActivityLog(currentInstance.value.id)
      // Server is source of truth — overwrite local state with persisted answers
      if (data.data.existingAnswers) {
        Object.entries(data.data.existingAnswers).forEach(([k, v]) => {
          answers[k] = v
        })
      }
    }
  } catch (e) {
    console.error('[Fill] Failed to refresh fill data:', e)
    ElMessage.error('Failed to refresh survey data')
  }
}

function switchPage(pi) {
  const page = (fillData.value?.pages || [])[pi]
  if (!page || isHidden(page)) return // Block navigation to hidden pages
  currentPageIdx.value = pi
  currentSectionIdx.value = 0
}

async function prevPage() {
  if (currentPageIdx.value <= 0) return
  await flushPendingSaves()
  // Find previous visible page
  for (let i = currentPageIdx.value - 1; i >= 0; i--) {
    const page = (fillData.value?.pages || [])[i]
    if (page && !isHidden(page)) {
      currentPageIdx.value = i
      currentSectionIdx.value = 0
      return
    }
  }
}
async function nextPage() {
  await flushPendingSaves()
  const pages = fillData.value?.pages || []
  // Find next visible page
  for (let i = currentPageIdx.value + 1; i < pages.length; i++) {
    const page = pages[i]
    if (page && !isHidden(page)) {
      currentPageIdx.value = i
      currentSectionIdx.value = 0
      return
    }
  }
}

// Guard against stale responses when user rapidly clicks different surveys
let openSurveySeq = 0

async function openSurvey(instanceId) {
  const seq = ++openSurveySeq
  currentInstance.value = instances.value.find(i => i.id === instanceId)
  currentPageIdx.value = 0
  try {
    const { data } = await getFillDataApi(instanceId)
    if (seq !== openSurveySeq) return // stale — user navigated to a different survey
    if (data.code === 200) {
      fillData.value = data.data
      // Server is source of truth for persisted answers — overwrite local state
      if (data.data.existingAnswers) {
        Object.entries(data.data.existingAnswers).forEach(([k, v]) => { answers[k] = v })
      }
      // Navigate to first visible page
      const pages = data.data.pages || []
      const ht = data.data.hiddenTargets || []
      let firstVisible = 0
      for (let i = 0; i < pages.length; i++) {
        if (!ht.includes('PAGE:' + pages[i].id)) { firstVisible = i; break }
      }
      currentPageIdx.value = firstVisible
      fetchActivityLog(instanceId)
    } else {
      ElMessage.error(data.message || 'Failed to load survey')
      currentInstance.value = null
    }
  } catch (e) {
    ElMessage.error('Failed to load survey: ' + (e.response?.data?.message || e.message))
    currentInstance.value = null
  }
}

async function fetchActivityLog(instanceId) {
  try {
    const { data } = await getInstanceLogApi(instanceId)
    if (data.code === 200) activityLog.value = data.data || []
  } catch (e) { activityLog.value = [] }
}

async function handleHeadAction(command) {
  try {
    if (command === "reopen-page") {
      await handleReopenPage()
    } else if (command === "reopen-instance") {
      const { data } = await reopenInstanceApi(currentInstance.value.id)
      if (data.code === 200) {
        ElMessage.success("Instance reopened for editing")
        await refreshFillData()
      } else {
        ElMessage.error(data.message || "Failed to reopen instance")
      }
    } else if (command === "complete-instance") {
      await handleSubmit()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "Action failed")
  }
}

async function handleCompletePage() {
  await flushPendingSaves()
  completingPage.value = true
  try {
    const page = fillData.value.pages[currentPageIdx.value]
    const { data } = await completePageApi(currentInstance.value.id, page.id)
    if (data.code === 200) {
      ElMessage.success('Page completed')
      // Refresh fill data to update page statuses
      await refreshFillData()
    } else {
      ElMessage.error(data.message || 'Failed to complete page')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to complete page')
  } finally { completingPage.value = false }
}

async function handleReopenPage() {
  await flushPendingSaves()
  completingPage.value = true
  try {
    const page = fillData.value.pages[currentPageIdx.value]
    const { data } = await reopenPageApi(currentInstance.value.id, page.id)
    if (data.code === 200) {
      ElMessage.success("Page reopened for editing")
      await refreshFillData()
    } else {
      ElMessage.error(data.message || "Failed to reopen page")
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || "Failed to reopen page")
  } finally { completingPage.value = false }
}

async function handleSubmit() {
  submitting.value = true
  try {
    const { data } = await submitSurveyApi(currentInstance.value.id, {})
    if (data.code === 200) {
      ElMessage.success('Survey submitted!')
      currentInstance.value = null
      fillData.value = null
    } else {
      ElMessage.error(data.message || 'Submission failed')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Submission failed')
  } finally { submitting.value = false }
}

async function loadInstances() {
  loading.value = true
  try {
    const { data } = await getMyInstancesApi()
    if (data.code === 200) instances.value = data.data || []
  } catch (e) {
    console.error('[Fill] Failed to load instances:', e)
    ElMessage.error('Failed to load pending surveys')
  } finally { loading.value = false }
}

onMounted(() => loadInstances())

// Clean up pending debounced save timers when leaving the fill view
onBeforeUnmount(() => {
  if (setAnswer._pending) {
    Object.values(setAnswer._pending).forEach(entry => {
      if (entry?.timer) clearTimeout(entry.timer)
    })
    setAnswer._pending = {}
  }
})

// Refresh instance list when navigating back to list (e.g., after reassign)
watch(currentInstance, (newVal, oldVal) => {
  if (!newVal && oldVal) loadInstances()
})
</script>

<style scoped>
.fill-page { max-width: 960px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); min-height: 100vh; }

/* Skeleton loading */
.fill-loading { padding: var(--space-2xl) 0; }
.skeleton-list { display: flex; flex-direction: column; gap: var(--space-lg); }
.skeleton-card { padding: var(--space-xl); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); }
.skeleton-line { height: 14px; border-radius: var(--radius-sm); background: var(--color-gray-200); margin-bottom: var(--space-sm); animation: skeleton-pulse 1.5s ease-in-out infinite; }
.skeleton-line--med { width: 55%; }
.skeleton-line--long { width: 80%; }
@keyframes skeleton-pulse { 0%, 100% { opacity: 0.4; } 50% { opacity: 0.8; } }

/* Instance list */
.fill-instances-title { font-family: var(--font-heading); font-size: var(--text-2xl); margin: 0 0 var(--space-lg); }
.empty-state { text-align: center; padding: var(--space-3xl); color: var(--color-text-secondary); }
.empty-icon { width: 48px; height: 48px; color: var(--color-gray-300); margin-bottom: var(--space-md); }
.empty-title { font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); margin: 0 0 var(--space-xs); }
.empty-desc { font-size: var(--text-sm); color: var(--color-text-secondary); margin: 0 0 var(--space-lg); max-width: 360px; margin-left: auto; margin-right: auto; }
.instance-list { display: flex; flex-direction: column; gap: var(--space-md); }
.instance-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); cursor: pointer; transition: all var(--transition-fast); position: relative; }
.instance-card:hover { border-color: var(--color-primary-light); box-shadow: var(--shadow-md); transform: translateY(-1px); }
.instance-card h3 { margin: 0 0 var(--space-sm); font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); }
.instance-meta { display: flex; gap: var(--space-md); font-size: var(--text-sm); color: var(--color-text-secondary); align-items: center; flex-wrap: wrap; }
.instance-template { color: var(--color-text-secondary); font-size: var(--text-xs); background: var(--color-gray-50); padding: 2px 8px; border-radius: var(--radius-full); }
.instance-assignee { display: inline-flex; align-items: center; gap: 4px; font-size: var(--text-xs); color: var(--color-text-secondary); }
.instance-assignee svg { flex-shrink: 0; }

/* Fill layout */
.fill-layout { display: flex; flex-direction: column; min-height: calc(100vh - 64px); }

/* Top bar — just back link */
.fill-topbar { padding: var(--space-sm) 0 var(--space-md); }
.fill-back { display: inline-flex; align-items: center; gap: 6px; padding: 6px 4px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-secondary); background: none; border: none; cursor: pointer; transition: color var(--transition-fast); }
.fill-back:hover { color: var(--color-primary); }
.fill-back svg { width: 16px; height: 16px; }

/* Survey Head card */
.survey-head { position: relative; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); margin-bottom: var(--space-lg); box-shadow: var(--shadow-sm); }
.survey-head-title { font-size: var(--text-xl); font-weight: 700; font-family: var(--font-heading); margin: 0 0 var(--space-md); color: var(--color-text-primary); }
.survey-head-cols { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-xl); margin-bottom: var(--space-md); }
.survey-head-col { display: flex; flex-direction: column; gap: 8px; }
.survey-head-item { display: flex; align-items: center; gap: var(--space-sm); }
.survey-head-label { font-size: var(--text-xs); font-weight: 500; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; min-width: 90px; }
.survey-head-value { display: inline-flex; align-items: center; gap: 4px; font-size: var(--text-sm); font-weight: 500; color: var(--color-text-primary); }
.survey-head-value svg { flex-shrink: 0; color: var(--color-text-muted); }
.survey-head-na { font-size: var(--text-sm); color: var(--color-text-muted); }
.survey-head-page-status { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); white-space: nowrap; }
.survey-head-page-status--ready_to_start { background: var(--color-gray-100); color: var(--color-text-secondary); }
.survey-head-page-status--in_progress { background: #FEF3C7; color: #92400E; }
.survey-head-page-status--submitted, .survey-head-page-status--completed { background: #D1FAE5; color: #047857; }
.survey-head-page-status--reopen { background: #FEF3C7; color: #92400E; }
.survey-head-progress {}
.survey-head-progress-bar { width: 100%; height: 6px; background: var(--color-gray-200); border-radius: var(--radius-full); overflow: hidden; }
.survey-head-progress-fill { height: 100%; background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light)); border-radius: var(--radius-full); transition: width var(--transition-base); }

/* Actions dropdown */
.survey-head-actions { position: absolute; top: var(--space-lg); right: var(--space-lg); }
.head-action-btn { display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; padding: 0; background: none; border: 1px solid transparent; border-radius: var(--radius-md); cursor: pointer; color: var(--color-text-muted); transition: all var(--transition-fast); }
.head-action-btn:hover { border-color: var(--color-gray-200); color: var(--color-text-primary); background: var(--color-gray-50); }

/* Reassign button */
.reassign-btn { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; padding: 0; background: none; border: 1px solid transparent; border-radius: var(--radius-sm); cursor: pointer; color: var(--color-text-muted); transition: all var(--transition-fast); flex-shrink: 0; }
.reassign-btn:hover { border-color: var(--color-gray-200); color: var(--color-primary); background: var(--color-primary-bg); }

.reassign-loading { text-align: center; padding: var(--space-xl); color: var(--color-text-muted); }
.reassign-empty { display: flex; flex-direction: column; align-items: center; gap: var(--space-sm); padding: var(--space-xl); color: var(--color-text-muted); }
.reassign-empty-icon { font-size: var(--text-xl); opacity: 0.5; }
.reassign-user-list { display: flex; flex-direction: column; gap: 2px; }
.reassign-user-group-label { font-size: 10px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; padding: 8px 14px 4px; border-top: 1px solid var(--color-gray-100); margin-top: 4px; }
.reassign-user-group-label:first-child { border-top: none; margin-top: 0; }
.reassign-user-item { display: flex; align-items: center; gap: var(--space-sm); padding: 10px 14px; background: none; border: none; cursor: pointer; border-radius: var(--radius-md); font-family: var(--font-body); font-size: var(--text-sm); transition: background var(--transition-fast); width: 100%; text-align: left; }
.reassign-user-item:hover { background: var(--color-primary-bg); }
.reassign-user-avatar { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: var(--radius-full); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); color: var(--color-white); font-size: var(--text-xs); font-weight: 700; flex-shrink: 0; }
.reassign-user-info { flex: 1; display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.reassign-user-name { font-weight: 500; color: var(--color-text-primary); }
.reassign-user-role { font-size: var(--text-xs); color: var(--color-text-muted); }

.fill-body { display: flex; flex-direction: column; flex: 1; }

/* Content card — same width as head */
.fill-content-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; }

/* Page tabs — inside card */
.fill-page-tabs { display: flex; align-items: center; gap: 4px; padding: var(--space-sm) var(--space-lg); overflow-x: auto; border-bottom: 2px solid var(--color-gray-200); }
.fill-tab { display: flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: var(--radius-md) var(--radius-md) 0 0; cursor: pointer; font-size: var(--text-sm); font-family: var(--font-body); font-weight: 500; background: none; border: none; color: var(--color-text-secondary); transition: all var(--transition-fast); white-space: nowrap; min-height: 40px; }
.fill-tab:hover:not(:disabled) { background: var(--color-gray-50); color: var(--color-text-primary); }
.fill-tab--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 700; box-shadow: inset 0 -2px 0 var(--color-primary); }
.fill-tab:disabled { opacity: 0.3; cursor: not-allowed; }
.fill-tab-num { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; border-radius: 50%; flex-shrink: 0; color: var(--color-text-secondary); background: var(--color-gray-200); border: 2px solid transparent; transition: all var(--transition-fast); }
.fill-tab--active .fill-tab-num { background: var(--color-primary); color: white; }

/* Page status indicator — colored ring */
.fill-tab-num--ready_to_start { border-color: var(--color-gray-400); }
.fill-tab-num--in_progress { border-color: #F59E0B; }
.fill-tab-num--submitted, .fill-tab-num--completed { border-color: #10B981; }
.fill-tab--active .fill-tab-num { border-color: transparent; }

/* Section tabs — inside card */
.fill-section-tabs { display: flex; align-items: center; gap: 4px; padding: var(--space-sm) var(--space-lg) 0; overflow-x: auto; }
.fill-section-tabs + .fill-content { border-top: 1px solid var(--color-gray-200); }
.fill-section-tab { display: flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: var(--radius-md) var(--radius-md) 0 0; cursor: pointer; font-size: var(--text-xs); font-family: var(--font-body); font-weight: 500; background: none; border: none; color: var(--color-text-secondary); transition: all var(--transition-fast); white-space: nowrap; min-height: 36px; }
.fill-section-tab:hover { background: var(--color-gray-50); color: var(--color-text-primary); }
.fill-section-tab--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 700; box-shadow: inset 0 -2px 0 var(--color-primary); }
.fill-section-tab-num { width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: 700; background: var(--color-gray-200); border-radius: 50%; flex-shrink: 0; }
.fill-section-tab--active .fill-section-tab-num { background: var(--color-primary); color: white; }

/* Content — inside card */
.fill-content { flex: 1; min-width: 0; padding: var(--space-xl) var(--space-lg) var(--space-2xl); }

.fill-question { margin-bottom: var(--space-lg); }
.fill-q-label { display: block; font-size: var(--text-sm); font-weight: 500; margin-bottom: 6px; color: var(--color-text-primary); }
.fill-q-required { color: var(--color-danger); }

.fill-options { display: flex; flex-direction: column; gap: 4px; }
.fill-opt { display: flex; align-items: center; gap: 12px; font-size: var(--text-sm); cursor: pointer; padding: 10px 14px; border: 1px solid transparent; border-radius: var(--radius-md); transition: background var(--transition-fast), border-color var(--transition-fast); }
.fill-opt:hover { background: var(--color-gray-50); border-color: var(--color-gray-200); }
.fill-opt:focus-within { border-color: var(--color-primary); background: var(--color-primary-bg); }
.fill-opt input[type="radio"], .fill-opt input[type="checkbox"] { width: 20px; height: 20px; accent-color: var(--color-primary); cursor: pointer; flex-shrink: 0; }
.opt-dot { width: 20px; height: 20px; border: 2px solid var(--color-gray-300); border-radius: 50%; flex-shrink: 0; transition: all var(--transition-fast); position: relative; }
.opt-dot::after { content: ''; position: absolute; inset: 4px; border-radius: 50%; background: var(--color-primary); transform: scale(0); transition: transform var(--transition-fast); }
.opt-dot--checked { border-color: var(--color-primary); }
.opt-dot--checked::after { transform: scale(1); }

.fill-rating { display: flex; gap: 2px; }
.fill-rating-btn { padding: 8px; min-width: 44px; min-height: 44px; display: flex; align-items: center; justify-content: center; background: none; border: 2px solid transparent; border-radius: var(--radius-sm); cursor: pointer; color: var(--color-gray-300); transition: color var(--transition-fast), transform var(--transition-fast), border-color var(--transition-fast); }
.fill-rating-btn:hover { transform: scale(1.1); color: #F59E0B; border-color: var(--color-gray-200); }
.fill-rating-btn:focus-visible { border-color: var(--color-primary); outline: none; }
.fill-rating-btn--active { color: #F59E0B; }
.fill-rating-btn--active:hover { border-color: #F59E0B40; }

/* Disabled state for choices & rating when user is not the page assignee */
.fill-options--disabled { opacity: 0.55; pointer-events: none; }
.fill-rating--disabled { opacity: 0.55; pointer-events: none; }

.fill-nav-btns { display: flex; justify-content: space-between; align-items: center; margin-top: var(--space-xl); padding-top: var(--space-lg); border-top: 1px solid var(--color-gray-200); }
.fill-nav-center { display: flex; gap: var(--space-sm); }

/* ── Activity Timeline ── */
.activity-section { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-md); margin-top: var(--space-lg); box-shadow: var(--shadow-sm); }
.activity-toggle { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 4px 0; background: none; border: none; cursor: pointer; font-family: var(--font-body); font-size: var(--text-sm); font-weight: 600; color: var(--color-text-secondary); min-height: 44px; }
.activity-toggle:hover { color: var(--color-text-primary); }
.activity-toggle-arrow { transition: transform 200ms; color: var(--color-text-muted); flex-shrink: 0; }
.activity-toggle-arrow--open { transform: rotate(180deg); }
.activity-timeline { margin-top: var(--space-sm); padding-left: 12px; border-left: 2px solid var(--color-gray-200); margin-left: 7px; display: flex; flex-direction: column; gap: var(--space-sm); }
.activity-entry { display: flex; gap: var(--space-sm); position: relative; }
.activity-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 4px; margin-left: -18px; box-shadow: 0 0 0 3px var(--color-white); }
.activity-dot--success { background: #10B981; }
.activity-dot--warning { background: #F59E0B; }
.activity-dot--info { background: #3B82F6; }
.activity-dot--default { background: var(--color-gray-400); }
.activity-body { flex: 1; min-width: 0; }
.activity-header { display: flex; justify-content: space-between; align-items: baseline; gap: var(--space-sm); }
.activity-action { font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); white-space: nowrap; }
.activity-time { font-size: var(--text-xs); color: var(--color-text-muted); white-space: nowrap; }
.activity-detail { font-size: var(--text-xs); color: var(--color-text-muted); margin-top: 2px; line-height: 1.4; word-break: break-word; }

/* Shared */
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-ready_to_start, .status-in_progress { background: #FEF3C7; color: #92400E; }
.status-submitted { background: #DBEAFE; color: #1D4ED8; }
.status-completed { background: #D1FAE5; color: #047857; }

.input { padding: 10px 14px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.textarea { resize: vertical; }
.muted { color: var(--color-text-secondary) !important; padding: 14px 16px; background: var(--color-gray-50); border: 1px dashed var(--color-gray-200); border-radius: var(--radius-md); font-size: var(--text-sm); }

.btn-primary { padding: 10px 24px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); border: none; border-radius: var(--radius-md); cursor: pointer; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { padding: 10px 24px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.btn-secondary:hover:not(:disabled) { background: var(--color-gray-50); }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

/* Transition animations */
.fade-enter-active, .fade-leave-active { transition: opacity var(--transition-fast), transform var(--transition-fast); }
.fade-enter-from { opacity: 0; transform: translateY(6px); }
.fade-leave-to { opacity: 0; transform: translateY(-6px); }

@media (max-width: 768px) {
  .fill-page { padding: var(--space-md); }
  .fill-body { flex-direction: column; }
  .survey-head { padding: var(--space-md); }
  .survey-head-cols { grid-template-columns: 1fr; gap: var(--space-md); }
  .survey-head-title { font-size: var(--text-lg); }
  .fill-content-card { border-radius: var(--radius-md); }
  .fill-page-tabs { padding: var(--space-xs) var(--space-md) 0 var(--space-xs); gap: 2px; }
  .fill-section-tabs { padding: var(--space-xs) var(--space-md) 0; gap: 2px; }
  .fill-tab { padding: 8px 10px; font-size: var(--text-xs); }
  .fill-section-tab { padding: 6px 10px; font-size: 11px; }
  .fill-content { padding: var(--space-md); }
  .fill-nav-btns { flex-direction: column; gap: var(--space-sm); }
  .fill-nav-btns .btn-primary, .fill-nav-btns .btn-secondary { width: 100%; }
}
</style>
