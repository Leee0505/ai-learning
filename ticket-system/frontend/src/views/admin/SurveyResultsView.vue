<template>
  <div class="view-page">
    <div class="view-topbar">
      <button class="btn-back" @click="$router.push('/admin/surveys')" aria-label="Back to templates">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        Templates
      </button>
      <h1 class="view-title">{{ template?.title }}</h1>
      <span :class="['status-badge', 'status-' + (template?.status || '').toLowerCase()]">{{ template?.status }}</span>
    </div>

    <div v-if="!template" class="loading">Loading...</div>

    <div v-else class="view-body">
        <div class="preview-page-tabs">
          <button v-for="(page, pi) in template.pages" :key="'pv'+page.id"
                  class="preview-tab" :class="{ 'preview-tab--active': currentPageIdx === pi }"
                  @click="currentPageIdx = pi">
            <span class="preview-tab-num">{{ pi + 1 }}</span>
            {{ page.title }}
            <span class="preview-tab-qs">{{ countPageQuestions(page) }}</span>
          </button>
        </div>
        <main class="preview-content"
          <div v-if="currentPage" class="preview-page">
            <h2 class="preview-page-title">{{ currentPage.title }}</h2>
            <div v-for="section in currentPage.sections" :key="'ps'+section.id" class="preview-section">
              <div class="preview-section-head">
                <h3 class="preview-section-title">{{ section.title }}</h3>
                <span v-if="section.description" class="preview-section-desc">{{ section.description }}</span>
              </div>
              <div v-for="q in section.questions" :key="'pq'+q.id" class="preview-q">
                <div class="preview-q-top">
                  <span class="preview-q-num">{{ getQIndex(q) + 1 }}</span>
                  <div class="preview-q-info">
                    <span class="preview-q-title">{{ q.title }}<span v-if="q.required" class="preview-required"> *</span></span>
                    <span v-if="q.description" class="preview-q-desc">{{ q.description }}</span>
                  </div>
                  <span :class="['type-badge', 'qtype-' + q.type.toLowerCase()]">{{ q.type.replace('_',' ') }}</span>
                </div>

                <!-- TEXT -->
                <div v-if="q.type === 'TEXT'" class="preview-input">Text answer</div>

                <!-- TEXTAREA -->
                <div v-else-if="q.type === 'TEXTAREA'" class="preview-input preview-input--area">Long answer</div>

                <!-- DATE -->
                <div v-else-if="q.type === 'DATE'" class="preview-input">YYYY-MM-DD</div>

                <!-- SINGLE_CHOICE -->
                <div v-else-if="q.type === 'SINGLE_CHOICE'" class="preview-opts">
                  <div v-for="opt in getOptionList(q.options)" :key="opt.key" class="preview-opt">
                    <span class="opt-radio"></span> {{ opt.label }}
                  </div>
                </div>

                <!-- MULTI_CHOICE -->
                <div v-else-if="q.type === 'MULTI_CHOICE'" class="preview-opts">
                  <div v-for="opt in getOptionList(q.options)" :key="opt.key" class="preview-opt">
                    <span class="opt-checkbox"></span> {{ opt.label }}
                  </div>
                </div>

                <!-- DROPDOWN -->
                <div v-else-if="q.type === 'DROPDOWN'" class="preview-dropdown">
                  {{ getOptionList(q.options)[0]?.label || 'Select...' }}
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="preview-dropdown-arrow"><polyline points="6,9 12,15 18,9"/></svg>
                </div>

                <!-- RATING -->
                <div v-else-if="q.type === 'RATING'" class="preview-rating">
                  <svg v-for="i in (getRatingMax(q.options))" :key="i" viewBox="0 0 24 24" :fill="i <= (getRatingDefault(q.options) || 3) ? '#F59E0B' : 'none'" stroke="#D1D5DB" stroke-width="1.5" width="32" height="32"><polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/></svg>
                </div>

                <!-- TABLE -->
                <div v-else-if="q.type === 'TABLE'" class="preview-table-wrap">
                  <table class="preview-table">
                    <thead><tr><th v-for="col in getTableColumns(q.options)" :key="col.key" class="preview-th">
                      <div class="preview-th-label">{{ col.label }}</div>
                      <div class="preview-th-type">{{ col.type }}</div>
                    </th></tr></thead>
                    <tbody><tr v-for="r in (getTableRows(q.options))" :key="r"><td v-for="col in getTableColumns(q.options)" :key="col.key" class="preview-td">—</td></tr></tbody>
                  </table>
                  <div v-if="hasDropdownCols(q.options)" class="preview-table-col-detail">
                    <div v-for="col in getDropdownCols(q.options)" :key="col.key" class="preview-col-info">
                      <strong>{{ col.label }}:</strong> {{ (col.options || []).join(', ') || 'no options' }}
                    </div>
                  </div>
                </div>

                <!-- CASCADER -->
                <div v-else class="preview-input muted">{{ q.type.replace('_',' ') }}</div>

                <!-- Visibility Rules -->
                <div v-if="q.visibilityRules?.length" class="preview-rules">
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
        </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTemplateApi } from '@/api/survey'

const route = useRoute()
const template = ref(null)
const currentPageIdx = ref(0)
const expandedRules = reactive({})

const currentPage = computed(() => template.value?.pages?.[currentPageIdx.value] || null)

function toggleRules(qid) { expandedRules[qid] = !expandedRules[qid] }
function countPageQuestions(page) { let n = 0; (page.sections || []).forEach(s => n += (s.questions || []).length); return n }
function getQIndex(q) {
  if (!currentPage.value) return 0
  let idx = 0
  for (const s of currentPage.value.sections || []) {
    for (const qq of s.questions || []) {
      if (qq.id === q.id) return idx
      idx++
    }
  }
  return idx
}

function getOptionList(json) { try { const o = JSON.parse(json || '{}'); const raw = o.options || []; return raw.length ? (typeof raw[0] === 'object' ? raw : raw.map(s => ({ key: s, label: s }))) : [] } catch { return [] } }
function getOptionLabel(qid, key) {
  for (const p of template.value?.pages || [])
    for (const s of p.sections || [])
      for (const q of s.questions || [])
        if (q.id === qid) { const f = getOptionList(q.options).find(o => o.key === key); return f ? f.label : key }
  return key
}
function getQuestionTitle(qid) {
  for (const p of template.value?.pages || [])
    for (const s of p.sections || [])
      for (const q of s.questions || []) if (q.id === qid) return q.title
  return 'Q#' + qid
}
function formatOp(op) { const m = { eq:'equals', neq:'not equals', contains:'contains', not_contains:'does not contain', in:'includes', not_in:'does not include', gt:'>', gte:'≥', lt:'<', lte:'≤', answered:'is answered', not_answered:'is not answered', is_empty:'is empty', not_empty:'is not empty' }; return m[op] || op }
function getRatingMax(json) { try { return JSON.parse(json || '{}').max || 5 } catch { return 5 } }
function getRatingDefault(json) { try { return JSON.parse(json || '{}').value || 3 } catch { return 3 } }
function getTableColumns(json) { try { return JSON.parse(json || '{}').columns || [{ key:'c1', label:'Col 1', type:'TEXT' }] } catch { return [{ key:'c1', label:'Col 1', type:'TEXT' }] } }
function getTableRows(json) { try { return JSON.parse(json || '{}').rows || 3 } catch { return 3 } }
function hasDropdownCols(json) { return getDropdownCols(json).length > 0 }
function getDropdownCols(json) { return getTableColumns(json).filter(c => c.type === 'DROPDOWN') }

onMounted(async () => {
  const id = route.params.id
  try { const t = await getTemplateApi(id); if (t.data?.code === 200) template.value = t.data.data } catch { }
})
</script>

<style scoped>
.view-page { max-width: 860px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); min-height: 100vh; }
.view-topbar { display: flex; align-items: center; gap: var(--space-md); margin-bottom: var(--space-xl); }
.btn-back { display: flex; align-items: center; gap: 4px; padding: 8px 16px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; transition: all var(--transition-fast); }
.btn-back svg { width: 16px; height: 16px; }
.btn-back:hover { border-color: var(--color-primary); color: var(--color-primary); }
.view-title { flex: 1; font-family: var(--font-heading); font-size: var(--text-xl); font-weight: 700; margin: 0; }
.loading { text-align: center; padding: var(--space-3xl); color: var(--color-text-muted); }

.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 3px 10px; border-radius: var(--radius-full); text-transform: capitalize; }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.status-archived { background: var(--color-gray-100); color: var(--color-text-secondary); }

/* Top tabs */
.preview-page-tabs { display: flex; align-items: center; gap: 4px; margin-bottom: var(--space-lg); border-bottom: 2px solid var(--color-gray-200); padding-bottom: var(--space-sm); overflow-x: auto; }
.preview-tab { display: flex; align-items: center; gap: 8px; padding: 10px 18px; border-radius: var(--radius-md) var(--radius-md) 0 0; cursor: pointer; font-size: var(--text-sm); font-family: var(--font-body); font-weight: 500; background: none; border: none; color: var(--color-text-secondary); transition: all var(--transition-fast); white-space: nowrap; }
.preview-tab:hover { background: var(--color-gray-50); color: var(--color-text-primary); }
.preview-tab--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 700; box-shadow: inset 0 -2px 0 var(--color-primary); }
.preview-tab-num { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; border-radius: 50%; background: var(--color-gray-200); flex-shrink: 0; }
.preview-tab--active .preview-tab-num { background: var(--color-primary); color: white; }
.preview-tab-qs { font-size: 10px; color: var(--color-text-muted); margin-left: 2px; }
.preview-tab--active .preview-tab-qs { color: var(--color-primary); opacity: 0.7; }
.preview-content { padding-bottom: var(--space-3xl); }

/* Page */
.preview-page { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-xl); overflow: hidden; box-shadow: var(--shadow-sm); }
.preview-page-title { font-family: var(--font-heading); font-size: var(--text-lg); font-weight: 700; margin: 0; padding: var(--space-xl) var(--space-2xl); background: linear-gradient(135deg, var(--color-primary-bg), white); border-bottom: 1px solid var(--color-gray-100); }

/* Section */
.preview-section { padding: 0 var(--space-2xl); }
.preview-section + .preview-section { border-top: 1px solid var(--color-gray-100); }
.preview-section-head { padding: var(--space-xl) 0 var(--space-md); }
.preview-section-title { font-size: var(--text-xs); font-weight: 700; color: var(--color-text-muted); margin: 0; text-transform: uppercase; letter-spacing: 1px; }
.preview-section-desc { font-size: var(--text-sm); color: var(--color-text-secondary); margin: 6px 0 0; line-height: 1.5; }

/* Question */
.preview-q { padding: 0 0 var(--space-xl); }
.preview-q + .preview-q { border-top: 1px solid var(--color-gray-50); padding-top: var(--space-xl); }
.preview-q-top { display: flex; gap: var(--space-md); margin-bottom: var(--space-md); }
.preview-q-num { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; font-size: var(--text-xs); font-weight: 700; color: var(--color-primary); background: var(--color-primary-bg); border-radius: 50%; flex-shrink: 0; }
.preview-q-info { flex: 1; }
.preview-q-title { font-size: var(--text-base); font-weight: 500; color: var(--color-text-primary); }
.preview-required { color: var(--color-danger); }
.preview-q-desc { font-size: var(--text-xs); color: var(--color-text-muted); margin-top: 2px; }

/* Form controls */
.preview-input { padding: 12px 16px; font-size: var(--text-sm); color: var(--color-text-muted); background: var(--color-gray-50); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); }
.preview-input--area { min-height: 72px; }
.preview-opts { display: flex; flex-direction: column; gap: 10px; }
.preview-opt { display: flex; align-items: center; gap: 10px; font-size: var(--text-sm); color: var(--color-text-primary); }
.opt-radio { width: 18px; height: 18px; border: 2px solid var(--color-gray-300); border-radius: 50%; flex-shrink: 0; }
.opt-checkbox { width: 18px; height: 18px; border: 2px solid var(--color-gray-300); border-radius: 3px; flex-shrink: 0; }
.preview-dropdown { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; font-size: var(--text-sm); color: var(--color-text-muted); background: var(--color-gray-50); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); }
.preview-dropdown-arrow { width: 16px; height: 16px; color: var(--color-gray-400); }
.preview-rating { display: flex; gap: 6px; }

/* Table */
.preview-table-wrap { border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); overflow: hidden; }
.preview-table { width: 100%; border-collapse: collapse; font-size: var(--text-xs); }
.preview-th { padding: 8px 12px; background: var(--color-gray-50); border-bottom: 2px solid var(--color-gray-200); text-align: left; }
.preview-th-label { font-weight: 600; color: var(--color-text-primary); }
.preview-th-type { font-size: 9px; color: var(--color-text-muted); text-transform: uppercase; font-weight: 400; margin-top: 1px; }
.preview-td { padding: 8px 12px; border-bottom: 1px solid var(--color-gray-100); color: var(--color-text-muted); }
.preview-table-col-detail { padding: 10px 14px; background: var(--color-gray-50); border-top: 1px solid var(--color-gray-100); display: flex; flex-wrap: wrap; gap: 8px 16px; }
.preview-col-info { font-size: var(--text-xs); color: var(--color-text-secondary); }

/* Rules */
.preview-rules { margin-top: var(--space-md); }
.rules-toggle { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; font-size: var(--text-xs); font-family: var(--font-body); color: var(--color-primary); background: var(--color-primary-bg); border: none; border-radius: var(--radius-sm); cursor: pointer; transition: background var(--transition-fast); }
.rules-toggle:hover { background: #EDE9FE; }
.rules-toggle-icon { width: 14px; height: 14px; transition: transform var(--transition-fast); }
.rules-toggle-icon--open { transform: rotate(180deg); }
.rules-detail { margin-top: 8px; padding: 10px 14px; background: #F5F3FF; border-radius: var(--radius-md); border: 1px solid #EDE9FE; display: flex; flex-direction: column; gap: 6px; }
.rule-detail-row { font-size: var(--text-xs); color: var(--color-text-secondary); line-height: 1.5; }

/* Type badges */
.type-badge { font-size: 10px; font-weight: 600; padding: 3px 8px; border-radius: var(--radius-full); text-transform: uppercase; flex-shrink: 0; align-self: center; }
.qtype-single_choice { background: #DBEAFE; color: #1D4ED8; }
.qtype-multi_choice { background: #D1FAE5; color: #047857; }
.qtype-text { background: var(--color-gray-100); color: var(--color-text-secondary); }
.qtype-textarea { background: var(--color-gray-100); color: var(--color-text-secondary); }
.qtype-date { background: #FEF3C7; color: #92400E; }
.qtype-dropdown { background: #EDE9FE; color: #6D28D9; }
.qtype-cascader { background: #FCE7F3; color: #9D174D; }
.qtype-rating { background: #FFF7ED; color: #C2410C; }
.qtype-table { background: #E0F2FE; color: #0369A1; }

.muted { color: var(--color-text-muted); }

@media (max-width: 768px) {
  .view-page { padding: var(--space-md); }
  .preview-page-tabs { gap: 2px; }
  .preview-tab { padding: 8px 12px; font-size: var(--text-xs); }
  .preview-tab-qs { display: none; }
  .preview-section { padding: 0 var(--space-lg); }
  .preview-page-title { padding: var(--space-lg); }
}
</style>
