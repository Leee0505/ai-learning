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
      <div class="preview-layout">
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
                <label class="preview-q-label">
                  {{ q.title }}
                  <span v-if="q.required" class="preview-required">*</span>
                </label>
                <p v-if="q.description" class="preview-q-desc">{{ q.description }}</p>

                <!-- TEXT -->
                <input v-if="q.type === 'TEXT'" class="input" placeholder="Text answer" disabled />

                <!-- TEXTAREA -->
                <textarea v-else-if="q.type === 'TEXTAREA'" class="input textarea" rows="3" placeholder="Long answer" disabled></textarea>

                <!-- DATE -->
                <input v-else-if="q.type === 'DATE'" class="input" type="text" placeholder="YYYY-MM-DD" disabled />

                <!-- SINGLE_CHOICE -->
                <div v-else-if="q.type === 'SINGLE_CHOICE'" class="preview-opts">
                  <label v-for="opt in getOptionList(q.options)" :key="opt.key" class="preview-opt">
                    <span class="radio"></span> {{ opt.label }}
                  </label>
                </div>

                <!-- MULTI_CHOICE -->
                <div v-else-if="q.type === 'MULTI_CHOICE'" class="preview-opts">
                  <label v-for="opt in getOptionList(q.options)" :key="opt.key" class="preview-opt">
                    <span class="checkbox-box"></span> {{ opt.label }}
                  </label>
                </div>

                <!-- DROPDOWN -->
                <select v-else-if="q.type === 'DROPDOWN'" class="input" disabled>
                  <option>{{ getOptionList(q.options)[0]?.label || 'Select...' }}</option>
                </select>

                <!-- RATING -->
                <div v-else-if="q.type === 'RATING'" class="preview-rating">
                  <svg v-for="i in (getRatingMax(q.options))" :key="i" viewBox="0 0 24 24" :fill="i <= getRatingDefault(q.options) ? '#F59E0B' : 'none'" stroke="#D1D5DB" stroke-width="1.5" width="28" height="28"><polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/></svg>
                </div>

                <!-- TABLE -->
                <table v-else-if="q.type === 'TABLE'" class="preview-table">
                  <thead><tr><th v-for="col in getTableColumns(q.options)" :key="col.key">{{ col.label }}</th></tr></thead>
                  <tbody><tr v-for="r in (getTableRows(q.options))" :key="r"><td v-for="col in getTableColumns(q.options)" :key="col.key">—</td></tr></tbody>
                </table>

                <!-- CASCADER / other -->
                <div v-else class="input muted">{{ q.type.replace('_',' ') }} input</div>

                <!-- Visibility Rules -->
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

function getOptionList(json) {
  try { const o = JSON.parse(json || '{}'); const raw = o.options || []; return raw.length === 0 ? [] : typeof raw[0] === 'object' ? raw : raw.map(s => ({ key: s, label: s })) } catch { return [] }
}
function getOptionLabel(qid, key) {
  if (!template.value?.pages) return key
  for (const p of template.value.pages)
    for (const s of p.sections || [])
      for (const q of s.questions || [])
        if (q.id === qid) { const f = getOptionList(q.options).find(o => o.key === key); return f ? f.label : key }
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
function getRatingMax(json) { try { return JSON.parse(json || '{}').max || 5 } catch { return 5 } }
function getRatingDefault(json) { try { return JSON.parse(json || '{}').value || 3 } catch { return 3 } }
function getTableColumns(json) { try { return JSON.parse(json || '{}').columns || [{ key:'c1', label:'Col 1', type:'TEXT' }] } catch { return [{ key:'c1', label:'Col 1', type:'TEXT' }] } }
function getTableRows(json) { try { return JSON.parse(json || '{}').rows || 3 } catch { return 3 } }

onMounted(async () => {
  const id = route.params.id
  try { const t = await getTemplateApi(id); if (t.data?.code === 200) template.value = t.data.data } catch { }
})
</script>

<style scoped>
.view-page { max-width: 900px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.view-topbar { display: flex; align-items: center; gap: var(--space-md); margin-bottom: var(--space-lg); }
.btn-back { display: flex; align-items: center; padding: 6px 12px; background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.btn-back svg { width: 16px; height: 16px; }
.view-title { flex: 1; font-size: var(--text-xl); font-weight: 700; margin: 0; }
.loading { text-align: center; padding: var(--space-2xl); color: var(--color-text-muted); }
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.status-archived { background: var(--color-gray-100); color: var(--color-text-secondary); }

.preview-layout { display: flex; gap: var(--space-xl); }
.preview-nav { width: 180px; flex-shrink: 0; display: flex; flex-direction: column; gap: 2px; }
.preview-nav-btn { display: flex; align-items: center; gap: 8px; padding: 10px 12px; font-size: var(--text-sm); font-family: var(--font-body); background: none; border: none; border-radius: var(--radius-md); cursor: pointer; color: var(--color-text-secondary); text-align: left; transition: background var(--transition-fast); }
.preview-nav-btn:hover { background: var(--color-gray-50); }
.preview-nav-btn--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 600; }
.preview-nav-num { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-size: var(--text-xs); font-weight: 700; border-radius: 50%; background: var(--color-gray-200); flex-shrink: 0; }
.preview-nav-btn--active .preview-nav-num { background: var(--color-primary); color: white; }
.preview-nav-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.preview-content { flex: 1; min-width: 0; }
.preview-page { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-2xl); box-shadow: var(--shadow-sm); }
.preview-page-title { font-size: var(--text-xl); font-weight: 700; margin: 0 0 var(--space-2xl); padding-bottom: var(--space-md); border-bottom: 1px solid var(--color-gray-200); }

.preview-section { margin-bottom: var(--space-2xl); }
.preview-section-title { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-secondary); margin: 0 0 var(--space-xs); text-transform: uppercase; letter-spacing: 0.5px; }
.preview-section-desc { font-size: var(--text-xs); color: var(--color-text-muted); margin: 0 0 var(--space-md); }

.preview-q { padding: var(--space-lg); margin-bottom: var(--space-md); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); border-left: 3px solid var(--color-gray-300); }
.preview-q-label { display: block; font-size: var(--text-sm); font-weight: 500; color: var(--color-text-primary); margin-bottom: 8px; }
.preview-q-desc { font-size: var(--text-xs); color: var(--color-text-muted); margin: -4px 0 8px; }
.preview-required { color: var(--color-danger); font-weight: 700; margin-left: 2px; }

/* Form inputs (read-only preview) */
.input { padding: 10px 14px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; background: var(--color-gray-50); color: var(--color-text-muted); }
.textarea { resize: vertical; }
.muted { background: var(--color-gray-50); padding: 10px 14px; font-size: var(--text-sm); }
.preview-opts { display: flex; flex-direction: column; gap: 8px; }
.preview-opt { display: flex; align-items: center; gap: 8px; font-size: var(--text-sm); color: var(--color-text-primary); }
.radio, .checkbox-box { width: 16px; height: 16px; border: 2px solid var(--color-gray-300); border-radius: 50%; flex-shrink: 0; }
.checkbox-box { border-radius: 3px; }
.preview-rating { display: flex; gap: 4px; }
.preview-table { width: 100%; border-collapse: collapse; font-size: var(--text-xs); }
.preview-table th, .preview-table td { border: 1px solid var(--color-gray-200); padding: 6px 10px; text-align: left; }
.preview-table th { background: var(--color-gray-50); font-weight: 600; color: var(--color-text-secondary); }
.preview-table td { color: var(--color-text-muted); }

/* Rules */
.preview-q-rules { margin-top: 10px; padding-top: 8px; border-top: 1px dashed var(--color-gray-200); }
.rules-toggle { display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px; font-size: var(--text-xs); font-family: var(--font-body); color: var(--color-primary); background: var(--color-primary-bg); border: none; border-radius: var(--radius-sm); cursor: pointer; }
.rules-toggle:hover { background: #EDE9FE; }
.rules-toggle-icon { width: 14px; height: 14px; transition: transform var(--transition-fast); }
.rules-toggle-icon--open { transform: rotate(180deg); }
.rules-detail { margin-top: 6px; padding: 8px 10px; background: var(--color-gray-50); border-radius: var(--radius-sm); display: flex; flex-direction: column; gap: 4px; }
.rule-detail-row { font-size: var(--text-xs); color: var(--color-text-secondary); }

@media (max-width: 768px) {
  .preview-layout { flex-direction: column; }
  .preview-nav { width: 100%; flex-direction: row; overflow-x: auto; }
  .preview-nav-label { display: none; }
}
</style>
