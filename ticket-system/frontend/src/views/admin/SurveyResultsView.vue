<template>
  <div class="view-page">
    <div class="view-topbar">
      <button class="btn-back" @click="$router.push('/admin/surveys')" aria-label="Back to templates">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        Templates
      </button>
      <h1 class="view-title">{{ template?.title }}</h1>
      <span class="preview-mode-badge" title="Preview mode — form controls are not interactive">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
        Preview
      </span>
      <span :class="['status-badge', 'status-' + (template?.status || '').toLowerCase()]">{{ template?.status }}</span>
    </div>

    <!-- Loading skeleton -->
    <div v-if="!template" class="loading">
      <div class="skeleton-list">
        <div v-for="i in 3" :key="i" class="skeleton-card">
          <div class="skeleton-line skeleton-line--short"></div>
          <div class="skeleton-line skeleton-line--long"></div>
          <div class="skeleton-line skeleton-line--med"></div>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!template.pages?.length" class="empty-state">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon" aria-hidden="true"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
      <p class="empty-title">No pages in this template</p>
      <p class="empty-desc">This survey template has no pages yet. Edit the template to add content.</p>
    </div>

    <div v-else class="view-body">
        <div class="preview-page-tabs">
          <button v-for="(page, pi) in template.pages" :key="'pv'+page.id"
                  class="preview-tab" :class="{ 'preview-tab--active': currentPageIdx === pi }"
                  @click="currentPageIdx = pi; currentSectionIdx = 0">
            <span class="preview-tab-num">{{ pi + 1 }}</span>
            {{ page.title }}
            <span class="preview-tab-qs">{{ countPageQuestions(page) }}</span>
          </button>
          <button v-if="currentPage?.visibilityRules?.length" class="rule-badge" @click.stop="openRuleDialog('Page', currentPage.title, currentPage.visibilityRules)" :aria-label="'Page rules'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            {{ currentPage.visibilityRules.length }}
          </button>
        </div>
        <main class="preview-content">
          <div v-if="currentPage" class="preview-page">
            <!-- Section progress indicator -->
            <div v-if="currentPage.sections?.length > 1" class="preview-progress">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="15" y2="13"/></svg>
              Section {{ currentSectionIdx + 1 }} of {{ currentPage.sections.length }}
            </div>
            <!-- Section tabs -->
            <div v-if="currentPage.sections?.length" class="preview-section-tabs">
              <button v-for="(section, si) in currentPage.sections" :key="'pst'+section.id"
                      class="preview-section-tab" :class="{ 'preview-section-tab--active': currentSectionIdx === si }"
                      @click="currentSectionIdx = si">
                <span class="preview-section-tab-num">{{ si + 1 }}</span>
                {{ section.title }}
                <span class="preview-section-tab-qs">{{ section.questions?.length || 0 }} Q</span>
                <button v-if="section.visibilityRules?.length" class="rule-badge rule-badge--tiny" @click.stop="openRuleDialog('Section', section.title, section.visibilityRules)" :aria-label="'Section rules'" :title="section.visibilityRules.length + ' rule(s)'">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="10" height="10"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  {{ section.visibilityRules.length }}
                </button>
              </button>
            </div>
            <!-- Current section -->
            <div v-if="currentViewSection" class="preview-section">
              <div v-if="!currentViewSection.questions?.length" class="preview-empty-section">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="32" height="32"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
                <p>No questions in this section</p>
              </div>
              <div v-for="q in currentViewSection.questions" :key="'pq'+q.id" class="preview-q">
                <div class="preview-q-top">
                  <span class="preview-q-num">{{ getQIndex(q) + 1 }}</span>
                  <div class="preview-q-info">
                    <span class="preview-q-title">{{ q.title }}<span v-if="q.required" class="preview-required"> *</span></span>
                    <span v-if="q.description" class="preview-q-desc">{{ q.description }}</span>
                  </div>
                  <span :class="['type-badge', 'qtype-' + q.type.toLowerCase()]">{{ q.type.replace('_',' ') }}</span>
                  <button v-if="q.visibilityRules?.length" class="rule-badge" @click="openRuleDialog('Question', q.title, q.visibilityRules)" :aria-label="q.visibilityRules.length + ' rule(s)'">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    {{ q.visibilityRules.length }}
                  </button>
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
              </div>
            </div>
          </div>
        </main>
    </div>

    <!-- Rule Dialog -->
    <Teleport to="body">
      <div v-if="ruleDialog.show" class="rule-overlay" @click.self="ruleDialog.show = false">
        <div class="rule-dialog" role="dialog" aria-modal="true" :aria-label="ruleDialog.title">
          <div class="rule-dialog-header">
            <div>
              <span class="rule-dialog-type">{{ ruleDialog.targetType }} Rules</span>
              <h3 class="rule-dialog-title">{{ ruleDialog.targetName }}</h3>
            </div>
            <button class="rule-dialog-close" @click="ruleDialog.show = false" aria-label="Close">&times;</button>
          </div>
          <div class="rule-dialog-body">
            <div v-if="!ruleDialog.rules.length" class="rule-dialog-empty">No rules configured</div>
            <template v-for="(grp, gi) in groupedDialogRules" :key="'g'+grp.group">
              <div class="rule-group-label">{{ gi === 0 ? 'AND — all must match' : 'OR — any can match' }}</div>
              <div v-for="item in grp.items" :key="'r'+item.ruleIndex" class="rule-dialog-item" :class="{ 'rule-dialog-item--expanded': ruleDialog.expanded.has(item.ruleIndex) }">
                <button class="rule-dialog-item-header" @click="toggleRuleDetail(item.ruleIndex)">
                  <span class="rule-dialog-item-num">{{ item.ruleIndex + 1 }}</span>
                  <span class="rule-dialog-item-summary">{{ formatRuleSentence(item.rule) }}</span>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="rule-dialog-chevron" :class="{ 'rule-dialog-chevron--open': ruleDialog.expanded.has(item.ruleIndex) }"><polyline points="6,9 12,15 18,9"/></svg>
                </button>
                <div v-if="ruleDialog.expanded.has(item.ruleIndex)" class="rule-dialog-item-detail">
                  <div class="rule-detail-grid">
                    <div class="rule-detail-cell">
                      <span class="rule-detail-label">Source Question</span>
                      <span class="rule-detail-value">{{ getQuestionTitle(item.rule.sourceQuestionId) }}</span>
                    </div>
                    <div class="rule-detail-cell">
                      <span class="rule-detail-label">Operator</span>
                      <span class="rule-detail-value">{{ formatOpLabel(item.rule.op) }}</span>
                    </div>
                    <div class="rule-detail-cell" v-if="item.rule.op !== 'answered' && item.rule.op !== 'not_answered' && item.rule.op !== 'is_empty' && item.rule.op !== 'not_empty'">
                      <span class="rule-detail-label">Value</span>
                      <span class="rule-detail-value">{{ getOptionLabel(item.rule.sourceQuestionId, item.rule.value) || item.rule.value || '—' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
          <div class="rule-dialog-footer">
            <button class="btn-secondary" @click="ruleDialog.show = false">Close</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTemplateApi } from '@/api/survey'

const route = useRoute()
const template = ref(null)
const currentPageIdx = ref(0)
const currentSectionIdx = ref(0)

const currentPage = computed(() => template.value?.pages?.[currentPageIdx.value] || null)
const currentViewSection = computed(() => currentPage.value?.sections?.[currentSectionIdx.value] || null)

const ruleDialog = reactive({
  show: false,
  targetType: '',
  targetName: '',
  rules: [],
  expanded: new Set()
})

const groupedDialogRules = computed(() => {
  const rules = ruleDialog.rules || []
  const groups = []
  const andItems = []; const orItems = []
  for (let i = 0; i < rules.length; i++) {
    const r = rules[i]
    if (r.ruleType === 'OR') {
      orItems.push({ rule: r, ruleIndex: i })
    } else {
      andItems.push({ rule: r, ruleIndex: i })
    }
  }
  if (andItems.length > 0) groups.push({ group: 'AND', items: andItems })
  if (orItems.length > 0) groups.push({ group: 'OR', items: orItems })
  return groups
})

function openRuleDialog(type, name, rules) {
  ruleDialog.targetType = type
  ruleDialog.targetName = name
  ruleDialog.rules = rules || []
  ruleDialog.expanded.clear()
  ruleDialog.show = true
}

function toggleRuleDetail(ri) {
  if (ruleDialog.expanded.has(ri)) {
    ruleDialog.expanded.delete(ri)
  } else {
    ruleDialog.expanded.add(ri)
  }
}

function countPageQuestions(page) { let n = 0; (page.sections || []).forEach(s => n += (s.questions || []).length); return n }
function getQIndex(q) {
  if (!currentViewSection.value) return 0
  const idx = (currentViewSection.value.questions || []).findIndex(qq => qq.id === q.id)
  return idx >= 0 ? idx : 0
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
function formatRuleSentence(rule) {
  const srcTitle = getQuestionTitle(rule.sourceQuestionId)
  const valLabel = getOptionLabel(rule.sourceQuestionId, rule.value)
  switch (rule.op) {
    case 'answered':    return `Shows when "${srcTitle}" is answered`
    case 'not_answered': return `Shows when "${srcTitle}" is not answered`
    case 'is_empty':    return `Shows when "${srcTitle}" is empty`
    case 'not_empty':   return `Shows when "${srcTitle}" is not empty`
  }
  const val = valLabel || rule.value || ''
  switch (rule.op) {
    case 'eq':          return `Shows when "${srcTitle}" = ${val}`
    case 'neq':         return `Shows when "${srcTitle}" ≠ ${val}`
    case 'contains':    return `Shows when "${srcTitle}" contains "${val}"`
    case 'not_contains': return `Shows when "${srcTitle}" does not contain "${val}"`
    case 'in':          return `Shows when "${srcTitle}" includes ${val}`
    case 'not_in':      return `Shows when "${srcTitle}" does not include ${val}`
    case 'gt':          return `Shows when "${srcTitle}" > ${val}`
    case 'gte':         return `Shows when "${srcTitle}" ≥ ${val}`
    case 'lt':          return `Shows when "${srcTitle}" < ${val}`
    case 'lte':         return `Shows when "${srcTitle}" ≤ ${val}`
    default:            return `Shows when "${srcTitle}" ${rule.op} ${val}`
  }
}
function formatOpLabel(op) {
  const m = {
    eq: 'Equals', neq: 'Not Equals', contains: 'Contains', not_contains: 'Does Not Contain',
    in: 'Includes', not_in: 'Does Not Include', gt: 'Greater Than', gte: 'Greater or Equal',
    lt: 'Less Than', lte: 'Less or Equal', answered: 'Is Answered', not_answered: 'Is Not Answered',
    is_empty: 'Is Empty', not_empty: 'Is Not Empty'
  }
  return m[op] || op
}
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
.view-page { max-width: 960px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); min-height: 100vh; }
.view-topbar { display: flex; align-items: center; gap: var(--space-md); margin-bottom: var(--space-xl); }
.btn-back { display: flex; align-items: center; gap: 4px; padding: 8px 16px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; transition: all var(--transition-fast); }
.btn-back svg { width: 16px; height: 16px; }
.btn-back:hover { border-color: var(--color-primary); color: var(--color-primary); }
.view-title { flex: 1; font-family: var(--font-heading); font-size: var(--text-xl); font-weight: 700; margin: 0; }
/* Skeleton loading */
.loading { padding: var(--space-2xl) 0; }
.skeleton-list { display: flex; flex-direction: column; gap: var(--space-lg); }
.skeleton-card { padding: var(--space-xl); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); }
.skeleton-line { height: 14px; border-radius: var(--radius-sm); background: var(--color-gray-200); margin-bottom: var(--space-sm); animation: skeleton-pulse 1.5s ease-in-out infinite; }
.skeleton-line--short { width: 30%; }
.skeleton-line--long { width: 80%; }
.skeleton-line--med { width: 55%; }
@keyframes skeleton-pulse { 0%, 100% { opacity: 0.4; } 50% { opacity: 0.8; } }

/* Empty state */
.empty-state { text-align: center; padding: var(--space-3xl) var(--space-xl); }
.empty-icon { width: 48px; height: 48px; color: var(--color-gray-300); margin-bottom: var(--space-md); }
.empty-title { font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); margin: 0 0 var(--space-xs); }
.empty-desc { font-size: var(--text-sm); color: var(--color-text-secondary); margin: 0; }

.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 3px 10px; border-radius: var(--radius-full); text-transform: capitalize; white-space: nowrap; }
.preview-mode-badge { display: inline-flex; align-items: center; gap: 4px; font-size: var(--text-xs); font-weight: 500; padding: 3px 10px; border-radius: var(--radius-full); background: #FEF3C7; color: #92400E; white-space: nowrap; }
.preview-mode-badge svg { flex-shrink: 0; }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.status-archived { background: var(--color-gray-100); color: var(--color-text-secondary); }

/* Top tabs */
.preview-page-tabs { display: flex; align-items: center; gap: 4px; margin-bottom: var(--space-sm); border-bottom: 2px solid var(--color-gray-200); padding-bottom: var(--space-sm); overflow-x: auto; }
.preview-tab { display: flex; align-items: center; gap: 8px; padding: 10px 18px; border-radius: var(--radius-md) var(--radius-md) 0 0; cursor: pointer; font-size: var(--text-sm); font-family: var(--font-body); font-weight: 500; background: none; border: none; color: var(--color-text-secondary); transition: all var(--transition-fast); white-space: nowrap; }
.preview-tab:hover { background: var(--color-gray-50); color: var(--color-text-primary); }
.preview-tab--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 700; box-shadow: inset 0 -2px 0 var(--color-primary); }
.preview-tab-num { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; border-radius: 50%; background: var(--color-gray-200); flex-shrink: 0; }
.preview-tab--active .preview-tab-num { background: var(--color-primary); color: white; }
.preview-tab-qs { font-size: 11px; color: var(--color-text-secondary); margin-left: 2px; }
.preview-tab--active .preview-tab-qs { color: var(--color-primary); opacity: 0.7; }
.preview-content { padding-bottom: var(--space-3xl); }

/* Section progress */
.preview-progress { display: flex; align-items: center; gap: 6px; font-size: var(--text-xs); color: var(--color-text-secondary); padding: var(--space-xs) var(--space-lg); }
.preview-progress svg { color: var(--color-text-muted); flex-shrink: 0; }

/* Section tabs */
.preview-section-tabs { display: flex; align-items: center; gap: 4px; margin-bottom: 0; padding: var(--space-sm) var(--space-lg) var(--space-sm); border-bottom: 1px solid var(--color-gray-200); overflow-x: auto; }
.preview-section-tab { display: flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: var(--radius-md) var(--radius-md) 0 0; cursor: pointer; font-size: var(--text-xs); font-family: var(--font-body); font-weight: 500; background: none; border: none; color: var(--color-text-secondary); transition: all var(--transition-fast); white-space: nowrap; min-height: 36px; }
.preview-section-tab:hover { background: var(--color-gray-50); color: var(--color-text-primary); }
.preview-section-tab--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 700; box-shadow: inset 0 -2px 0 var(--color-primary); }
.preview-section-tab-num { width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: 700; background: var(--color-gray-200); border-radius: 50%; flex-shrink: 0; }
.preview-section-tab--active .preview-section-tab-num { background: var(--color-primary); color: white; }
.preview-section-tab-qs { font-size: 11px; color: var(--color-text-secondary); }
.preview-section-tab--active .preview-section-tab-qs { color: var(--color-primary); opacity: 0.7; }

/* Page */
.preview-page { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-xl); overflow: hidden; box-shadow: var(--shadow-sm); }

/* Section */
.preview-section { padding: var(--space-xl) var(--space-lg) 0; }
.preview-empty-section { display: flex; flex-direction: column; align-items: center; gap: var(--space-sm); padding: var(--space-3xl) var(--space-md); color: var(--color-text-secondary); font-size: var(--text-sm); }
.preview-empty-section svg { color: var(--color-gray-300); }

/* Question */
.preview-q { padding: 0 0 var(--space-xl); position: relative; transition: background var(--transition-fast); border-radius: var(--radius-md); }
.preview-q:hover { background: var(--color-gray-50); }
.preview-q + .preview-q { border-top: 1px solid var(--color-gray-100); padding-top: var(--space-xl); }
.preview-q::before { content: ''; position: absolute; left: 0; top: var(--space-md); bottom: var(--space-md); width: 3px; border-radius: 3px; background: transparent; transition: background var(--transition-fast); }
.preview-q:hover::before { background: var(--color-primary-light); }
.preview-q-top { display: flex; gap: var(--space-md); margin-bottom: var(--space-md); align-items: center; }
.preview-q-num { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; font-size: var(--text-xs); font-weight: 700; color: var(--color-primary); background: var(--color-primary-bg); border-radius: 50%; flex-shrink: 0; }
.preview-q-info { flex: 1; }
.preview-q-title { font-size: var(--text-base); font-weight: 500; color: var(--color-text-primary); }
.preview-required { color: var(--color-danger); }
.preview-q-desc { font-size: var(--text-xs); color: var(--color-text-secondary); margin-top: 2px; }

/* Form controls */
.preview-input { padding: 12px 16px; font-size: var(--text-sm); color: var(--color-text-secondary); background: var(--color-gray-50); border: 1px dashed var(--color-gray-200); border-radius: var(--radius-md); }
.preview-input--area { min-height: 72px; }
.preview-opts { display: flex; flex-direction: column; gap: 10px; }
.preview-opt { display: flex; align-items: center; gap: 10px; font-size: var(--text-sm); color: var(--color-text-primary); }
.opt-radio { width: 18px; height: 18px; border: 2px solid var(--color-gray-300); border-radius: 50%; flex-shrink: 0; }
.opt-checkbox { width: 18px; height: 18px; border: 2px solid var(--color-gray-300); border-radius: 3px; flex-shrink: 0; }
.preview-dropdown { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; font-size: var(--text-sm); color: var(--color-text-secondary); background: var(--color-gray-50); border: 1px dashed var(--color-gray-200); border-radius: var(--radius-md); }
.preview-dropdown-arrow { width: 16px; height: 16px; color: var(--color-gray-400); }
.preview-rating { display: flex; gap: 6px; }

/* Table */
.preview-table-wrap { border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); overflow: hidden; }
.preview-table { width: 100%; border-collapse: collapse; font-size: var(--text-xs); }
.preview-th { padding: 8px 12px; background: var(--color-gray-50); border-bottom: 2px solid var(--color-gray-200); text-align: left; }
.preview-th-label { font-weight: 600; color: var(--color-text-primary); }
.preview-th-type { font-size: 11px; color: var(--color-text-secondary); text-transform: uppercase; font-weight: 400; margin-top: 1px; }
.preview-td { padding: 8px 12px; border-bottom: 1px solid var(--color-gray-100); color: var(--color-text-secondary); }
.preview-table-col-detail { padding: 10px 14px; background: var(--color-gray-50); border-top: 1px solid var(--color-gray-100); display: flex; flex-wrap: wrap; gap: 8px 16px; }
.preview-col-info { font-size: var(--text-xs); color: var(--color-text-secondary); }

/* Rule badge — icon+count button */
.rule-badge { display: inline-flex; align-items: center; gap: 4px; padding: 6px 12px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); color: var(--color-primary); background: var(--color-primary-bg); border: 1px solid transparent; border-radius: var(--radius-md); cursor: pointer; white-space: nowrap; min-height: 36px; transition: all var(--transition-fast); }
.rule-badge:hover { background: var(--color-primary); color: var(--color-white); border-color: var(--color-primary); }
.rule-badge svg { flex-shrink: 0; }
.rule-badge--tiny { padding: 4px 8px; font-size: 11px; min-height: 32px; }

/* Rule Dialog */
.rule-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(15, 23, 42, 0.4); display: flex; align-items: center; justify-content: center; padding: var(--space-lg); }
.rule-dialog { background: var(--color-white); border-radius: var(--radius-xl); box-shadow: 0 20px 60px rgba(0,0,0,0.15); width: 100%; max-width: 560px; max-height: 80vh; display: flex; flex-direction: column; overflow: hidden; }
.rule-dialog-header { display: flex; align-items: flex-start; justify-content: space-between; padding: var(--space-xl) var(--space-xl) var(--space-md); border-bottom: 1px solid var(--color-gray-100); }
.rule-dialog-type { font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
.rule-dialog-title { font-size: var(--text-lg); font-weight: 700; margin: 2px 0 0; color: var(--color-text-primary); }
.rule-dialog-close { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: var(--color-text-muted); background: none; border: none; cursor: pointer; border-radius: var(--radius-sm); }
.rule-dialog-close:hover { background: var(--color-gray-100); color: var(--color-text-primary); }
.rule-dialog-body { flex: 1; overflow-y: auto; padding: var(--space-md) var(--space-xl); display: flex; flex-direction: column; gap: 6px; }
.rule-dialog-empty { text-align: center; padding: var(--space-xl); color: var(--color-text-secondary); font-size: var(--text-sm); }
.rule-dialog-footer { padding: var(--space-md) var(--space-xl); border-top: 1px solid var(--color-gray-100); display: flex; justify-content: flex-end; }
.rule-group-label { font-size: 10px; font-weight: 700; padding: 4px 0 6px; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.3px; }

/* Rule item in dialog */
.rule-dialog-item { border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); overflow: hidden; transition: border-color var(--transition-fast); }
.rule-dialog-item:hover { border-color: var(--color-primary-light); }
.rule-dialog-item--expanded { border-color: var(--color-primary); }
.rule-dialog-item-header { display: flex; align-items: center; gap: 10px; width: 100%; padding: 12px 14px; background: none; border: none; cursor: pointer; font-family: var(--font-body); text-align: left; transition: background var(--transition-fast); min-height: 44px; }
.rule-dialog-item-header:hover { background: var(--color-gray-50); }
.rule-dialog-item-num { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; background: var(--color-gray-100); color: var(--color-text-secondary); border-radius: 50%; flex-shrink: 0; }
.rule-dialog-item-summary { flex: 1; font-size: var(--text-sm); color: var(--color-text-primary); line-height: 1.4; }
.rule-dialog-chevron { width: 16px; height: 16px; color: var(--color-text-secondary); flex-shrink: 0; transition: transform var(--transition-fast); }
.rule-dialog-chevron--open { transform: rotate(180deg); }

/* Expanded rule detail */
.rule-dialog-item-detail { padding: 0 14px 14px 46px; }
.rule-detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.rule-detail-cell { display: flex; flex-direction: column; gap: 2px; }
.rule-detail-label { font-size: 11px; font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.3px; }
.rule-detail-value { font-size: var(--text-sm); color: var(--color-text-primary); font-weight: 500; }

/* Type badges */
.type-badge { font-size: 11px; font-weight: 600; padding: 3px 8px; border-radius: var(--radius-full); text-transform: uppercase; flex-shrink: 0; }
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

/* Shared */
.btn-secondary { padding: 10px 20px; font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; font-size: var(--text-sm); min-height: 44px; }
.btn-secondary:hover { background: var(--color-gray-50); }

@media (max-width: 768px) {
  .view-page { padding: var(--space-md); }
  .view-topbar { flex-wrap: wrap; gap: var(--space-sm); }
  .view-title { font-size: var(--text-lg); min-width: 100%; order: -1; }
  .preview-mode-badge { font-size: 10px; padding: 2px 8px; }
  .preview-page-tabs { gap: 2px; }
  .preview-tab { padding: 8px 12px; font-size: var(--text-xs); }
  .preview-tab-qs { display: none; }
  .preview-section { padding: var(--space-md) var(--space-md) 0; }
  .preview-section-tabs { gap: 2px; }
  .preview-section-tab { padding: 6px 10px; font-size: 11px; }
  .preview-section-tab-qs { display: none; }
  .preview-q { padding: 0 0 var(--space-lg); }
  .preview-q-top { flex-wrap: wrap; gap: var(--space-sm); }
  .type-badge { font-size: 10px; }
  .rule-badge { font-size: 10px; padding: 4px 8px; min-height: 32px; }
  .rule-dialog { max-width: 100%; max-height: 90vh; margin: var(--space-md); border-radius: var(--radius-lg); }
  .rule-dialog-body { padding: var(--space-sm); }
  .rule-detail-grid { grid-template-columns: 1fr; }
}
</style>
