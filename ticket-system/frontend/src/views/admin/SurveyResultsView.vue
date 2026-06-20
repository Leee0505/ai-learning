<template>
  <div class="results-page">
    <div class="results-topbar">
      <button class="btn-back" @click="$router.push('/admin/surveys')" aria-label="Back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        Back
      </button>
      <h1 class="results-title">{{ template?.title || 'Loading...' }}</h1>
      <span :class="['status-badge', 'status-' + (template?.status || '').toLowerCase()]">{{ template?.status }}</span>
    </div>

    <div v-if="!template" class="loading">Loading...</div>

    <div v-else class="results-body">
      <!-- Template Structure -->
      <section class="template-structure">
        <h2 class="section-heading">Structure</h2>
        <div v-for="page in template.pages" :key="'p'+page.id" class="structure-page">
          <div class="structure-page-head">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="structure-page-icon" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14,2 14,8 20,8"/></svg>
            <h3 class="structure-page-title">{{ page.title }}</h3>
          </div>
          <div v-for="section in page.sections" :key="'s'+section.id" class="structure-section">
            <div class="structure-section-head">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="structure-section-icon" aria-hidden="true"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
              <h4 class="structure-section-title">{{ section.title }}</h4>
            </div>
            <div v-for="q in section.questions" :key="'q'+q.id" class="structure-q">
              <div class="structure-q-header">
                <span class="structure-q-title">{{ q.title }}</span>
                <span :class="['type-badge', 'qtype-' + q.type.toLowerCase()]">{{ q.type.replace('_',' ') }}</span>
                <span v-if="q.required" class="structure-required">Required</span>
              </div>
              <div v-if="q.description" class="structure-q-desc">{{ q.description }}</div>
              <div v-if="hasOpts(q.type) && q.options" class="structure-q-meta">
                <span class="meta-label">Options:</span> {{ formatOpts(q.options) }}
              </div>
              <div v-if="q.type === 'RATING' && q.options" class="structure-q-meta">
                <span class="meta-label">Scale:</span> 1 – {{ getRatingMax(q.options) }}
              </div>
              <div v-if="q.type === 'TABLE' && q.options" class="structure-q-meta">
                <span class="meta-label">Columns:</span> {{ getTableCols(q.options) }}<span v-if="getTableColTypes(q.options)"> ({{ getTableColTypes(q.options) }})</span>
                &nbsp;·&nbsp; <span class="meta-label">Rows:</span> {{ getTableRows(q.options) }}
              </div>
              <!-- Expandable rules -->
              <div v-if="q.visibilityRules?.length" class="structure-q-rules">
                <button class="rules-toggle" @click="toggleRules(q.id)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="rules-toggle-icon" :class="{ 'rules-toggle-icon--open': expandedRules[q.id] }"><polyline points="6,9 12,15 18,9"/></svg>
                  {{ q.visibilityRules.length }} visibility rule{{ q.visibilityRules.length > 1 ? 's' : '' }}
                </button>
                <div v-if="expandedRules[q.id]" class="rules-detail">
                  <div v-for="(rule, ri) in q.visibilityRules" :key="ri" class="rule-detail-row">
                    When <strong>{{ getQuestionTitle(rule.sourceQuestionId) }}</strong>
                    {{ formatOp(rule.op) }}
                    <strong v-if="rule.value">{{ getOptionLabel(rule.sourceQuestionId, rule.value) }}</strong>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Results -->
      <section>
        <h2 class="section-heading">
          Results
          <span class="results-summary-inline">{{ results?.completedInstances || 0 }}/{{ results?.totalInstances || 0 }} completed</span>
        </h2>
        <div v-if="!results?.questions?.length" class="empty-results">No instances distributed yet</div>
        <div v-else v-for="q in questionsWithLabels" :key="q.questionId" class="result-card">
          <h3 class="result-q-title">{{ q.title }} <span class="result-q-type">{{ q.type.replace('_',' ') }}</span></h3>
          <div v-if="q.choiceCounts" class="result-bars">
            <div v-for="(count, opt) in q.choiceCounts" :key="opt" class="result-bar-row">
              <span class="result-bar-label">{{ opt }}</span>
              <div class="result-bar-track">
                <div class="result-bar-fill" :style="{ width: barWidth(count, q.choiceCounts) + '%' }"></div>
                <span class="result-bar-count">{{ count }}</span>
              </div>
            </div>
          </div>
          <div v-if="q.textAnswers" class="result-texts">
            <div v-if="q.textAnswers.length === 0" class="muted">No answers yet</div>
            <div v-for="(ans, ai) in q.textAnswers" :key="ai" class="result-text-item">{{ ans }}</div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/request'
import { getTemplateApi } from '@/api/survey'

const route = useRoute()
const results = ref(null)
const template = ref(null)
const expandedRules = reactive({})

function toggleRules(qid) { expandedRules[qid] = !expandedRules[qid] }

function parseOptions(optionsJson) {
  try {
    const o = JSON.parse(optionsJson || '{}')
    const raw = o.options || []
    if (raw.length === 0) return []
    return typeof raw[0] === 'object' ? raw : raw.map(s => ({ key: s, label: s }))
  } catch { return [] }
}

function getOptionLabel(questionId, key) {
  if (!template.value?.pages) return key
  for (const p of template.value.pages) {
    for (const s of p.sections || []) {
      for (const q of s.questions || []) {
        if (q.id === questionId) {
          const opts = parseOptions(q.options)
          const found = opts.find(o => o.key === key)
          return found ? found.label : key
        }
      }
    }
  }
  return key
}

function getQuestionTitle(qid) {
  if (!template.value?.pages) return 'Q#' + qid
  for (const p of template.value.pages) {
    for (const s of p.sections || []) {
      for (const q of s.questions || []) {
        if (q.id === qid) return q.title
      }
    }
  }
  return 'Q#' + qid
}

function formatOp(op) {
  const labels = { eq:'equals', neq:'not equals', contains:'contains', not_contains:'does not contain', in:'includes', not_in:'does not include', gt:'>', gte:'≥', lt:'<', lte:'≤', answered:'is answered', not_answered:'is not answered', is_empty:'is empty', not_empty:'is not empty' }
  return labels[op] || op
}

const questionsWithLabels = computed(() => {
  if (!results.value?.questions) return []
  return results.value.questions.map(q => {
    if (q.choiceCounts) {
      const labeled = {}
      for (const [key, count] of Object.entries(q.choiceCounts)) {
        const label = getOptionLabel(q.questionId, key)
        labeled[label] = count
      }
      return { ...q, choiceCounts: labeled }
    }
    return q
  })
})

function hasOpts(t) { return ['SINGLE_CHOICE','MULTI_CHOICE','DROPDOWN','CASCADER'].includes(t) }
function formatOpts(json) { try { const o = JSON.parse(json || '{}'); const raw = o.options || []; return raw.map(r => typeof r === 'object' ? r.label : r).join(', ') } catch { return '' } }
function getRatingMax(json) { try { return JSON.parse(json || '{}').max || 5 } catch { return 5 } }
function getTableCols(json) { try { return (JSON.parse(json || '{}').columns || []).length } catch { return 0 } }
function getTableRows(json) { try { return JSON.parse(json || '{}').rows || 3 } catch { return 3 } }
function getTableColTypes(json) { try { return (JSON.parse(json || '{}').columns || []).map(c => c.type).join(', ') } catch { return '' } }

function barWidth(count, counts) {
  const max = Math.max(...Object.values(counts), 1)
  return Math.round((count / max) * 100)
}

onMounted(async () => {
  const id = route.params.id
  try {
    const t = await getTemplateApi(id)
    if (t.data?.code === 200) template.value = t.data.data
  } catch { /* no template */ }
  try {
    const { data } = await request.get(`/admin/surveys/${id}/results`)
    if (data.code === 200) results.value = data.data
  } catch { /* no results yet */ }
})
</script>

<style scoped>
.results-page { max-width: 860px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.results-topbar { display: flex; align-items: center; gap: var(--space-md); margin-bottom: var(--space-xl); }
.btn-back { display: flex; align-items: center; padding: 6px 12px; background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.btn-back svg { width: 16px; height: 16px; }
.results-title { flex: 1; font-size: var(--text-xl); font-weight: 700; margin: 0; }
.loading { text-align: center; padding: var(--space-2xl); color: var(--color-text-muted); }
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.status-archived { background: var(--color-gray-100); color: var(--color-text-secondary); }

/* Section headings */
.section-heading { font-size: var(--text-lg); font-weight: 600; margin: var(--space-2xl) 0 var(--space-md); padding-bottom: var(--space-sm); border-bottom: 2px solid var(--color-gray-200); display: flex; align-items: baseline; gap: var(--space-md); }
.results-summary-inline { font-size: var(--text-sm); font-weight: 400; color: var(--color-text-muted); }
.empty-results { text-align: center; padding: var(--space-xl); color: var(--color-text-muted); font-size: var(--text-sm); }

/* Template structure */
.structure-page { margin-bottom: var(--space-xl); }
.structure-page-head { display: flex; align-items: center; gap: 8px; margin-bottom: var(--space-sm); }
.structure-page-icon { width: 20px; height: 20px; color: var(--color-primary); flex-shrink: 0; }
.structure-page-title { font-size: var(--text-base); font-weight: 700; margin: 0; }

.structure-section { margin-left: 28px; margin-bottom: var(--space-md); }
.structure-section-head { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.structure-section-icon { width: 16px; height: 16px; color: var(--color-text-muted); flex-shrink: 0; }
.structure-section-title { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-secondary); margin: 0; text-transform: uppercase; letter-spacing: 0.5px; }

.structure-q { padding: 10px 14px; margin-bottom: 6px; margin-left: 22px; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); border-left: 3px solid var(--color-gray-300); }
.structure-q-header { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.structure-q-title { font-size: var(--text-sm); font-weight: 500; }
.structure-q-desc { font-size: var(--text-xs); color: var(--color-text-muted); margin-top: 2px; }
.structure-q-meta { font-size: var(--text-xs); color: var(--color-text-secondary); margin-top: 4px; }
.meta-label { color: var(--color-text-muted); font-weight: 500; }

/* Rules */
.structure-q-rules { margin-top: 6px; }
.rules-toggle { display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px; font-size: var(--text-xs); font-family: var(--font-body); color: var(--color-primary); background: var(--color-primary-bg); border: none; border-radius: var(--radius-sm); cursor: pointer; }
.rules-toggle:hover { background: #EDE9FE; }
.rules-toggle-icon { width: 14px; height: 14px; transition: transform var(--transition-fast); }
.rules-toggle-icon--open { transform: rotate(180deg); }
.rules-detail { margin-top: 6px; padding: 8px 10px; background: var(--color-gray-50); border-radius: var(--radius-sm); display: flex; flex-direction: column; gap: 4px; }
.rule-detail-row { font-size: var(--text-xs); color: var(--color-text-secondary); }
.structure-required { font-size: 10px; font-weight: 600; color: var(--color-danger); background: #FEE2E2; padding: 1px 6px; border-radius: var(--radius-full); }

/* Results */
.result-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); margin-bottom: var(--space-lg); }
.result-q-title { font-size: var(--text-base); font-weight: 600; margin: 0 0 var(--space-md); }
.result-q-type { font-size: var(--text-xs); color: var(--color-text-muted); font-weight: 400; }
.result-bars { display: flex; flex-direction: column; gap: 8px; }
.result-bar-row { display: flex; align-items: center; gap: var(--space-md); }
.result-bar-label { width: 100px; font-size: var(--text-sm); color: var(--color-text-secondary); text-align: right; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.result-bar-track { flex: 1; height: 28px; background: var(--color-gray-100); border-radius: var(--radius-sm); position: relative; overflow: hidden; display: flex; align-items: center; }
.result-bar-fill { height: 100%; background: var(--color-primary); border-radius: var(--radius-sm); opacity: 0.8; min-width: 2px; transition: width 0.5s ease; }
.result-bar-count { position: absolute; right: 8px; font-size: var(--text-xs); font-weight: 600; color: var(--color-text-primary); }
.result-texts { display: flex; flex-direction: column; gap: 6px; }
.result-text-item { padding: 8px 12px; font-size: var(--text-sm); background: var(--color-gray-50); border-radius: var(--radius-sm); color: var(--color-text-primary); }
.muted { font-size: var(--text-sm); color: var(--color-text-muted); }

/* Type badges */
.type-badge { font-size: 10px; font-weight: 600; padding: 2px 6px; border-radius: var(--radius-full); text-transform: uppercase; }
.qtype-single_choice { background: #DBEAFE; color: #1D4ED8; }
.qtype-multi_choice { background: #D1FAE5; color: #047857; }
.qtype-text { background: var(--color-gray-100); color: var(--color-text-secondary); }
.qtype-textarea { background: var(--color-gray-100); color: var(--color-text-secondary); }
.qtype-date { background: #FEF3C7; color: #92400E; }
.qtype-dropdown { background: #EDE9FE; color: #6D28D9; }
.qtype-cascader { background: #FCE7F3; color: #9D174D; }
.qtype-rating { background: #FFF7ED; color: #C2410C; }
.qtype-table { background: #E0F2FE; color: #0369A1; }

@media (max-width: 768px) {
  .structure-section { margin-left: 16px; }
  .structure-q { margin-left: 10px; }
  .result-bar-label { width: 60px; font-size: var(--text-xs); }
}
</style>
