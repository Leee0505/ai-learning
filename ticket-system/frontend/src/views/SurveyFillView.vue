<template>
  <div class="fill-page">
    <!-- Loading -->
    <div v-if="loading" class="fill-loading">Loading survey...</div>

    <!-- Instance list (when no instance selected) -->
    <div v-else-if="!currentInstance" class="fill-instances">
      <h1 class="fill-instances-title">My Surveys</h1>
      <div v-if="instances.length === 0" class="empty-state">
        <p>No pending surveys</p>
      </div>
      <div v-else class="instance-list">
        <div v-for="inst in instances" :key="inst.id" class="instance-card" @click="openSurvey(inst.id)">
          <h3>{{ inst.title }}</h3>
          <div class="instance-meta">
            <span :class="['status-badge', 'status-' + inst.status.toLowerCase()]">{{ inst.status.replace(/_/g, ' ') }}</span>
            <span>{{ inst.completedPages }}/{{ inst.totalPages }} pages</span>
            <span v-if="inst.templateTitle" class="instance-template">{{ inst.templateTitle }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Fill view -->
    <div v-else class="fill-layout">
      <!-- Top bar -->
      <div class="fill-topbar">
        <button class="fill-back" @click="currentInstance = null" aria-label="Back to list">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        </button>
        <h2 class="fill-title">{{ currentInstance.title }}</h2>
        <span class="fill-page-indicator">Page {{ currentPageIdx + 1 }}/{{ visiblePages.length }}</span>
      </div>

      <div class="fill-body">
        <!-- Left: Page navigation -->
        <nav class="fill-nav" aria-label="Survey pages">
          <button v-for="(page, pi) in fillData?.pages" :key="page.id"
                  class="fill-nav-btn" :class="navClass(pi)" @click="switchPage(pi)"
                  :disabled="isHidden(page)" :aria-label="'Page ' + (pi+1) + ': ' + page.title">
            <span class="fill-nav-icon">{{ navIcon(pi) }}</span>
            <span class="fill-nav-label">{{ page.title }}</span>
          </button>
        </nav>

        <!-- Right: Questions -->
        <main class="fill-content">
          <div v-if="currentPage" class="fill-page-content">
            <div v-for="section in visibleSections" :key="section.id" class="fill-section">
              <h3 class="fill-section-title">{{ section.title }}</h3>
              <p v-if="section.description" class="fill-section-desc">{{ section.description }}</p>

              <div v-for="q in visibleQuestions(section)" :key="q.id" class="fill-question">
                <label :for="'q'+q.id" class="fill-q-label">
                  {{ q.title }}
                  <span v-if="q.required" class="fill-q-required">*</span>
                </label>

                <!-- TEXT -->
                <input v-if="q.type === 'TEXT'" :id="'q'+q.id" type="text" class="input"
                       :value="answers[q.id] || ''" @input="setAnswer(q.id, $event.target.value)" />

                <!-- TEXTAREA -->
                <textarea v-else-if="q.type === 'TEXTAREA'" :id="'q'+q.id" class="input textarea" rows="3"
                          :value="answers[q.id] || ''" @input="setAnswer(q.id, $event.target.value)"></textarea>

                <!-- DATE -->
                <input v-else-if="q.type === 'DATE'" :id="'q'+q.id" type="date" class="input"
                       :value="answers[q.id] || ''" @change="setAnswer(q.id, $event.target.value)" />

                <!-- SINGLE_CHOICE / DROPDOWN (rendered as radio group) -->
                <div v-else-if="q.type === 'SINGLE_CHOICE' || q.type === 'DROPDOWN'" class="fill-options">
                  <label v-for="(opt, oi) in parseOptions(q.options)" :key="oi" class="fill-opt">
                    <input type="radio" :name="'q'+q.id" :value="opt"
                           :checked="answers[q.id] === opt" @change="setAnswer(q.id, opt)" />
                    {{ opt }}
                  </label>
                </div>

                <!-- MULTI_CHOICE -->
                <div v-else-if="q.type === 'MULTI_CHOICE'" class="fill-options">
                  <label v-for="(opt, oi) in parseOptions(q.options)" :key="oi" class="fill-opt">
                    <input type="checkbox" :value="opt"
                           :checked="multiChecked(q.id, opt)" @change="toggleMulti(q.id, opt)" />
                    {{ opt }}
                  </label>
                </div>

                <!-- RATING -->
                <div v-else-if="q.type === 'RATING'" class="fill-rating">
                  <button v-for="i in getRatingMax(q.options)" :key="i" class="fill-rating-btn"
                          :class="{ 'fill-rating-btn--active': Number(answers[q.id] || 0) >= i }"
                          @click="setAnswer(q.id, String(i))" :aria-label="'Rate ' + i">
                    <svg viewBox="0 0 24 24" :fill="Number(answers[q.id] || 0) >= i ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.5" width="28" height="28"><polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/></svg>
                  </button>
                </div>

                <!-- CASCADER / TABLE (placeholder) -->
                <div v-else class="input muted">{{ q.type.replace('_',' ') }} — not yet supported</div>
              </div>
            </div>
          </div>

          <!-- Navigation buttons -->
          <div class="fill-nav-btns">
            <button :disabled="currentPageIdx === 0" class="btn-secondary" @click="prevPage">Previous</button>
            <button v-if="currentPageIdx < visiblePages.length - 1" class="btn-primary" @click="nextPage">Next</button>
            <button v-else class="btn-primary" @click="handleSubmit" :disabled="submitting">
              {{ submitting ? 'Submitting...' : 'Submit Survey' }}
            </button>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyInstancesApi, getFillDataApi, saveAnswerApi, submitSurveyApi } from '@/api/survey'

const loading = ref(true)
const submitting = ref(false)
const instances = ref([])
const currentInstance = ref(null)
const fillData = ref(null)
const currentPageIdx = ref(0)
const answers = reactive({})

const currentPage = computed(() => fillData.value?.pages?.[currentPageIdx.value])
const visiblePages = computed(() => (fillData.value?.pages || []).filter(p => !isHidden(p)))
const visibleSections = computed(() => (currentPage.value?.sections || []).filter(s => !fillData.value?.hiddenTargets?.includes('SECTION:' + s.id)))

function isHidden(target) {
  if (!fillData.value?.hiddenTargets) return false
  if (target.hasOwnProperty('id')) {
    return fillData.value.hiddenTargets.has('PAGE:' + target.id) ||
           fillData.value.hiddenTargets.has('SECTION:' + target.id)
  }
  return false
}

function visibleQuestions(section) {
  return (section.questions || []).filter(q => !fillData.value?.hiddenTargets?.has('QUESTION:' + q.id))
}

function parseOptions(optionsJson) {
  try { return JSON.parse(optionsJson || '{}').options || [] }
  catch { return [] }
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
  setAnswer(qid, vals.join(','))
}

function setAnswer(qid, value) {
  answers[qid] = value
  // Auto-save after 500ms debounce
  if (setAnswer._timers) clearTimeout(setAnswer._timers[qid])
  if (!setAnswer._timers) setAnswer._timers = {}
  setAnswer._timers[qid] = setTimeout(async () => {
    if (!currentInstance.value) return
    await saveAnswerApi(currentInstance.value.id, { questionId: qid, value })
    // Re-fetch to update visibility
    await refreshFillData()
  }, 500)
}

async function refreshFillData() {
  if (!currentInstance.value) return
  const { data } = await getFillDataApi(currentInstance.value.id)
  if (data.code === 200) {
    fillData.value = data.data
    // Merge existing answers
    if (data.data.existingAnswers) {
      Object.entries(data.data.existingAnswers).forEach(([k, v]) => {
        if (!answers[k]) answers[k] = v
      })
    }
  }
}

function navClass(pi) {
  if (pi === currentPageIdx.value) return 'nav-current'
  const page = fillData.value?.pages?.[pi]
  if (isHidden(page)) return 'nav-hidden'
  return 'nav-pending'
}

function navIcon(pi) {
  if (isHidden(fillData.value?.pages?.[pi])) return '—'
  const page = fillData.value?.pages?.[pi]
  if (!page) return '○'
  // Check if all required questions on page are answered
  const allAnswered = page.sections?.every(s =>
    (s.questions || []).every(q => {
      if (fillData.value?.hiddenTargets?.has('QUESTION:' + q.id)) return true
      if (!q.required) return true
      return answers[q.id] && answers[q.id].length > 0
    })
  )
  if (pi === currentPageIdx.value) return '◉'
  if (allAnswered) return '✓'
  return '○'
}

function switchPage(pi) {
  currentPageIdx.value = pi
}

function prevPage() {
  if (currentPageIdx.value > 0) currentPageIdx.value--
}
function nextPage() {
  // Flush pending auto-saves
  if (setAnswer._timers) {
    Object.values(setAnswer._timers).forEach(t => clearTimeout(t))
    setAnswer._timers = {}
  }
  if (currentPageIdx.value < visiblePages.value.length - 1) {
    currentPageIdx.value++
  }
}

async function openSurvey(instanceId) {
  currentInstance.value = instances.value.find(i => i.id === instanceId)
  currentPageIdx.value = 0
  const { data } = await getFillDataApi(instanceId)
  if (data.code === 200) {
    fillData.value = data.data
    if (data.data.existingAnswers) {
      Object.entries(data.data.existingAnswers).forEach(([k, v]) => { answers[k] = v })
    }
  }
}

async function handleSubmit() {
  // Flush pending saves
  if (setAnswer._timers) {
    Object.values(setAnswer._timers).forEach(t => clearTimeout(t))
    setAnswer._timers = {}
  }
  submitting.value = true
  try {
    const answerList = Object.entries(answers).map(([qid, val]) => ({ questionId: Number(qid), value: val }))
    const { data } = await submitSurveyApi(currentInstance.value.id, { answers: answerList })
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

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getMyInstancesApi()
    if (data.code === 200) instances.value = data.data || []
  } catch { /* no instances */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.fill-page { max-width: 960px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); min-height: 100vh; }
.fill-loading { text-align: center; padding: var(--space-3xl); color: var(--color-text-muted); }

/* Instance list */
.fill-instances-title { font-family: var(--font-heading); font-size: var(--text-2xl); margin: 0 0 var(--space-lg); }
.empty-state { text-align: center; padding: var(--space-3xl); color: var(--color-text-muted); }
.instance-list { display: flex; flex-direction: column; gap: var(--space-md); }
.instance-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); cursor: pointer; transition: box-shadow var(--transition-fast); }
.instance-card:hover { box-shadow: var(--shadow-md); }
.instance-card h3 { margin: 0 0 var(--space-sm); font-size: var(--text-lg); }
.instance-meta { display: flex; gap: var(--space-md); font-size: var(--text-sm); color: var(--color-text-secondary); align-items: center; }
.instance-template { color: var(--color-text-muted); }

/* Fill layout */
.fill-layout { display: flex; flex-direction: column; min-height: calc(100vh - 64px); }
.fill-topbar { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md) 0; border-bottom: 1px solid var(--color-gray-200); margin-bottom: var(--space-lg); }
.fill-back { display: flex; align-items: center; padding: 6px 12px; background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.fill-back svg { width: 16px; height: 16px; }
.fill-title { flex: 1; font-size: var(--text-lg); font-weight: 600; margin: 0; }
.fill-page-indicator { font-size: var(--text-sm); color: var(--color-text-muted); }

.fill-body { display: flex; gap: var(--space-xl); flex: 1; }

/* Left nav */
.fill-nav { width: 200px; flex-shrink: 0; display: flex; flex-direction: column; gap: 4px; }
.fill-nav-btn { display: flex; align-items: center; gap: 8px; padding: 10px 12px; font-size: var(--text-sm); font-family: var(--font-body); background: none; border: none; border-radius: var(--radius-md); cursor: pointer; text-align: left; color: var(--color-text-secondary); transition: background var(--transition-fast); }
.fill-nav-btn:hover:not(:disabled) { background: var(--color-gray-50); }
.fill-nav-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.nav-current { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 600; }
.nav-hidden { display: none; }
.fill-nav-icon { font-size: var(--text-base); width: 20px; text-align: center; flex-shrink: 0; }
.fill-nav-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Content */
.fill-content { flex: 1; min-width: 0; padding-bottom: var(--space-2xl); }
.fill-section { margin-bottom: var(--space-xl); }
.fill-section-title { font-size: var(--text-base); font-weight: 600; margin: 0 0 var(--space-xs); color: var(--color-text-primary); padding-bottom: var(--space-sm); border-bottom: 1px solid var(--color-gray-100); }
.fill-section-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0 0 var(--space-md); }

.fill-question { margin-bottom: var(--space-lg); }
.fill-q-label { display: block; font-size: var(--text-sm); font-weight: 500; margin-bottom: 6px; color: var(--color-text-primary); }
.fill-q-required { color: var(--color-danger); }

.fill-options { display: flex; flex-direction: column; gap: 8px; }
.fill-opt { display: flex; align-items: center; gap: 8px; font-size: var(--text-sm); cursor: pointer; padding: 6px 0; }
.fill-opt input[type="radio"], .fill-opt input[type="checkbox"] { width: 18px; height: 18px; accent-color: var(--color-primary); cursor: pointer; }

.fill-rating { display: flex; gap: 4px; }
.fill-rating-btn { padding: 4px; background: none; border: none; cursor: pointer; color: var(--color-gray-300); transition: color var(--transition-fast), transform var(--transition-fast); }
.fill-rating-btn:hover { transform: scale(1.15); }
.fill-rating-btn--active { color: #F59E0B; }

.fill-nav-btns { display: flex; justify-content: space-between; margin-top: var(--space-xl); padding-top: var(--space-lg); border-top: 1px solid var(--color-gray-200); }

/* Shared */
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-ready_to_start, .status-in_progress { background: #FEF3C7; color: #92400E; }
.status-submitted { background: #DBEAFE; color: #1D4ED8; }
.status-completed { background: #D1FAE5; color: #047857; }

.input { padding: 10px 14px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.textarea { resize: vertical; }
.muted { color: var(--color-text-muted) !important; padding: 10px 0; }

.btn-primary { padding: 10px 24px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); border: none; border-radius: var(--radius-md); cursor: pointer; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { padding: 10px 24px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.btn-secondary:hover:not(:disabled) { background: var(--color-gray-50); }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 768px) {
  .fill-body { flex-direction: column; }
  .fill-nav { width: 100%; flex-direction: row; overflow-x: auto; gap: 2px; }
  .fill-nav-btn { flex-shrink: 0; }
  .fill-nav-label { display: none; }
}
</style>
