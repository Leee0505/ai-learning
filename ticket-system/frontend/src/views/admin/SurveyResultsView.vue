<template>
  <div class="view-page">
    <div class="view-topbar">
      <button class="btn-back" @click="$router.push('/admin/surveys')" aria-label="Back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        Back
      </button>
      <h1 class="view-title">{{ template?.title || 'Loading...' }}</h1>
      <span :class="['status-badge', 'status-' + (template?.status || '').toLowerCase()]">{{ template?.status }}</span>
    </div>

    <div v-if="!template" class="loading">Loading...</div>

    <div v-else class="view-body">
      <!-- Tabs -->
      <div class="view-tabs">
        <button :class="{ 'view-tab--active': activeTab === 'preview' }" class="view-tab" @click="activeTab = 'preview'">Preview</button>
        <button :class="{ 'view-tab--active': activeTab === 'results' }" class="view-tab" @click="activeTab = 'results'">
          Results
          <span class="view-tab-badge">{{ results?.completedInstances || 0 }}/{{ results?.totalInstances || 0 }}</span>
        </button>
      </div>

      <!-- Preview Tab: one page at a time -->
      <div v-if="activeTab === 'preview'" class="preview-layout">
        <nav class="preview-nav" aria-label="Survey pages">
          <button v-for="(page, pi) in template.pages" :key="'pv'+page.id"
                  class="preview-nav-btn" :class="{ 'preview-nav-btn--active': currentPageIdx === pi }"
                  @click="currentPageIdx = pi">
            <span class="preview-nav-num">{{ pi + 1 }}</span>
            <span class="preview-nav-label">{{ page.title }}</span>
          </button>
        </nav>
        <main class="preview-content">
          <div v-if="currentPage" class="preview-page">
            <h2 class="preview-page-title">{{ currentPage.title }}</h2>
            <div v-for="section in currentPage.sections" :key="'ps'+section.id" class="preview-section">
              <h3 class="preview-section-title">{{ section.title }}</h3>
              <p v-if="section.description" class="preview-section-desc">{{ section.description }}</p>
              <div v-for="q in section.questions" :key="'pq'+q.id" class="preview-q">
                <div class="preview-q-head">
                  <span class="preview-q-title">{{ q.title }}</span>
                  <span :class="['type-badge', 'qtype-' + q.type.toLowerCase()]">{{ q.type.replace('_',' ') }}</span>
                  <span v-if="q.required" class="preview-required">Required</span>
                </div>
                <div v-if="q.description" class="preview-q-desc">{{ q.description }}</div>
                <div v-if="hasOpts(q.type) && q.options" class="preview-q-meta">Options: {{ formatOpts(q.options) }}</div>
                <div v-if="q.type === 'RATING' && q.options" class="preview-q-meta">Scale: 1 – {{ getRatingMax(q.options) }}</div>
                <div v-if="q.type === 'TABLE' && q.options" class="preview-q-meta">{{ getTableCols(q.options) }} columns · {{ getTableRows(q.options) }} rows</div>

                <!-- Rules -->
                <div v-if="q.visibilityRules?.length" class="preview-q-rules">
                  <button class="rules-toggle" @click="toggleRules(q.id)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="rules-toggle-icon" :class="{ 'rules-toggle-icon--open': expandedRules[q.id] }"><polyline points="6,9 12,15 18,9"/></svg>
                    {{ q.visibilityRules.length }} rule{{ q.visibilityRules.length > 1 ? 's' : '' }}
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
        </main>
      </div>

      <!-- Results Tab: aggregated data -->
      <div v-if="activeTab === 'results'" class="results-body">
        <div v-if="!results?.questions?.length" class="empty-results">
          No instances distributed yet. Distribute this survey to start collecting responses.
        </div>
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
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/request'
import { getTemplateApi } from '@/api/survey'

const route = useRoute()
const template = ref(null)
const results = ref(null)
const activeTab = ref('preview')
const currentPageIdx = ref(0)
const expandedRules = reactive({})

const currentPage = computed(() => template.value?.pages?.[currentPageIdx.value] || null)

function toggleRules(qid) { expandedRules[qid] = !expandedRules[qid] }

function parseOptions(optionsJson) {
  try { const o = JSON.parse(optionsJson || '{}'); const raw = o.options || []; return raw.length === 0 ? [] : typeof raw[0] === 'object' ? raw : raw.map(s => ({ key: s, label: s })) } catch { return [] }
}
function getOptionLabel(questionId, key) {
  if (!template.value?.pages) return key
  for (const p of template.value.pages)
    for (const s of p.sections || [])
      for (const q of s.questions || [])
        if (q.id === questionId) { const found = parseOptions(q.options).find(o => o.key === key); return found ? found.label : key }
  return key
}
function getQuestionTitle(qid) {
  if (!template.value?.pages) return 'Q#' + qid
  for (const p of template.value.pages)
    for (const s of p.sections || [])
      for (const q of s.questions || [])
        if (q.id === qid) return q.title
  return 'Q#' + qid
}
function formatOp(op) {
  const m = { eq:'equals', neq:'not equals', contains:'contains', not_contains:'does not contain', in:'includes', not_in:'does not include', gt:'>', gte:'≥', lt:'<', lte:'≤', answered:'is answered', not_answered:'is not answered', is_empty:'is empty', not_empty:'is not empty' }
  return m[op] || op
}
const questionsWithLabels = computed(() => {
  if (!results.value?.questions) return []
  return results.value.questions.map(q => {
    if (q.choiceCounts) {
      const labeled = {}
      for (const [key, count] of Object.entries(q.choiceCounts)) labeled[getOptionLabel(q.questionId, key)] = count
      return { ...q, choiceCounts: labeled }
    }
    return q
  })
})
function hasOpts(t) { return ['SINGLE_CHOICE','MULTI_CHOICE','DROPDOWN','CASCADER'].includes(t) }
function formatOpts(json) { try { const o = JSON.parse(json || '{}'); return (o.options || []).map(r => typeof r === 'object' ? r.label : r).join(', ') } catch { return '' } }
function getRatingMax(json) { try { return JSON.parse(json || '{}').max || 5 } catch { return 5 } }
function getTableCols(json) { try { return (JSON.parse(json || '{}').columns || []).length } catch { return 0 } }
function getTableRows(json) { try { return JSON.parse(json || '{}').rows || 3 } catch { return 3 } }
function barWidth(count, counts) { return Math.round((count / Math.max(...Object.values(counts), 1)) * 100) }

onMounted(async () => {
  const id = route.params.id
  try { const t = await getTemplateApi(id); if (t.data?.code === 200) template.value = t.data.data } catch { }
  try { const { data } = await request.get(`/admin/surveys/${id}/results`); if (data.code === 200) results.value = data.data } catch { }
})
</script>

<style scoped>
.view-page { max-width: 960px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.view-topbar { display: flex; align-items: center; gap: var(--space-md); margin-bottom: var(--space-lg); }
.btn-back { display: flex; align-items: center; padding: 6px 12px; background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.btn-back svg { width: 16px; height: 16px; }
.view-title { flex: 1; font-size: var(--text-xl); font-weight: 700; margin: 0; }
.loading { text-align: center; padding: var(--space-2xl); color: var(--color-text-muted); }
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.status-archived { background: var(--color-gray-100); color: var(--color-text-secondary); }

/* Tabs */
.view-tabs { display: flex; gap: 0; border-bottom: 2px solid var(--color-gray-200); margin-bottom: var(--space-lg); }
.view-tab { padding: 10px 20px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: none; border: none; border-bottom: 2px solid transparent; margin-bottom: -2px; cursor: pointer; transition: color 150ms, border-color 150ms; display: flex; align-items: center; gap: 8px; }
.view-tab:hover { color: var(--color-primary); }
.view-tab--active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }
.view-tab-badge { font-size: var(--text-xs); background: var(--color-gray-100); padding: 1px 6px; border-radius: var(--radius-full); font-weight: 400; }

/* Preview: page nav + content */
.preview-layout { display: flex; gap: var(--space-xl); }
.preview-nav { width: 180px; flex-shrink: 0; display: flex; flex-direction: column; gap: 2px; }
.preview-nav-btn { display: flex; align-items: center; gap: 8px; padding: 10px 12px; font-size: var(--text-sm); font-family: var(--font-body); background: none; border: none; border-radius: var(--radius-md); cursor: pointer; color: var(--color-text-secondary); text-align: left; transition: background var(--transition-fast); }
.preview-nav-btn:hover { background: var(--color-gray-50); }
.preview-nav-btn--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 600; }
.preview-nav-num { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-size: var(--text-xs); font-weight: 700; border-radius: 50%; background: var(--color-gray-200); flex-shrink: 0; }
.preview-nav-btn--active .preview-nav-num { background: var(--color-primary); color: white; }
.preview-nav-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.preview-content { flex: 1; min-width: 0; }
.preview-page { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-xl); box-shadow: var(--shadow-sm); }
.preview-page-title { font-size: var(--text-xl); font-weight: 700; margin: 0 0 var(--space-xl); padding-bottom: var(--space-md); border-bottom: 1px solid var(--color-gray-200); }

.preview-section { margin-bottom: var(--space-xl); }
.preview-section-title { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-secondary); margin: 0 0 var(--space-xs); text-transform: uppercase; letter-spacing: 0.5px; }
.preview-section-desc { font-size: var(--text-xs); color: var(--color-text-muted); margin: 0 0 var(--space-sm); }

.preview-q { padding: 12px 14px; margin-bottom: 8px; background: var(--color-gray-50); border-radius: var(--radius-md); border-left: 3px solid var(--color-gray-300); }
.preview-q-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.preview-q-title { font-size: var(--text-sm); font-weight: 500; }
.preview-q-desc { font-size: var(--text-xs); color: var(--color-text-muted); margin-top: 2px; }
.preview-q-meta { font-size: var(--text-xs); color: var(--color-text-secondary); margin-top: 4px; }
.preview-required { font-size: 10px; font-weight: 600; color: var(--color-danger); background: #FEE2E2; padding: 1px 6px; border-radius: var(--radius-full); }

/* Rules */
.preview-q-rules { margin-top: 6px; }
.rules-toggle { display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px; font-size: var(--text-xs); font-family: var(--font-body); color: var(--color-primary); background: var(--color-primary-bg); border: none; border-radius: var(--radius-sm); cursor: pointer; }
.rules-toggle:hover { background: #EDE9FE; }
.rules-toggle-icon { width: 14px; height: 14px; transition: transform var(--transition-fast); }
.rules-toggle-icon--open { transform: rotate(180deg); }
.rules-detail { margin-top: 6px; padding: 8px 10px; background: var(--color-white); border-radius: var(--radius-sm); display: flex; flex-direction: column; gap: 4px; }
.rule-detail-row { font-size: var(--text-xs); color: var(--color-text-secondary); }

/* Results */
.results-body { display: flex; flex-direction: column; gap: var(--space-lg); }
.empty-results { text-align: center; padding: var(--space-2xl); color: var(--color-text-muted); font-size: var(--text-sm); }
.result-card { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); }
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
  .preview-layout { flex-direction: column; }
  .preview-nav { width: 100%; flex-direction: row; overflow-x: auto; }
  .preview-nav-label { display: none; }
  .result-bar-label { width: 60px; font-size: var(--text-xs); }
}
</style>
