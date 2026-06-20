<template>
  <div class="builder">
    <!-- Top bar -->
    <div class="builder-topbar">
      <button class="builder-back" @click="$router.push('/admin/surveys')" aria-label="Back to templates">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        Back
      </button>
      <h1 class="builder-title">{{ template?.title || 'Loading...' }}</h1>
      <span :class="['status-badge', 'status-' + (template?.status || '').toLowerCase()]">{{ template?.status }}</span>
      <div class="builder-topbar-spacer"></div>
      <button class="btn-secondary" @click="saveAndReturn">Done</button>
    </div>

    <div v-if="!template" class="builder-loading">Loading template...</div>

    <div v-else class="builder-layout">
      <!-- Left: Structure Tree (navigation only) -->
      <aside class="builder-left">
        <div class="builder-left-header">
          <h3 class="builder-left-title">Pages</h3>
          <button class="icon-btn" title="Add Page" @click="addPage" aria-label="Add page">+</button>
        </div>

        <!-- Page tabs -->
        <div class="page-tabs">
          <div v-for="(page, pi) in template.pages" :key="'pt'+page.id"
               class="page-tab" :class="{ 'page-tab--active': currentPageIndex === pi }"
               @click="switchPage(pi)">
            <span class="page-tab-num">{{ pi + 1 }}</span>
            <span class="page-tab-label">{{ page.title }}</span>
            <button v-if="template.pages.length > 1" class="page-tab-del" @click.stop="deletePage(page.id)" aria-label="Delete page">×</button>
          </div>
        </div>
      </aside>

      <!-- Center: Editable Form Canvas -->
      <main class="builder-center">
        <div v-if="currentPage" class="canvas">
          <!-- Page title (editable) -->
          <div class="canvas-page-header">
            <input v-model="currentPageTitle" class="canvas-page-title-input" @blur="savePageTitle" @keyup.enter="($event.target.blur())" placeholder="Page title" />
            <button class="canvas-add-section" @click="addSection(currentPage.id)">+ Add Section</button>
          </div>

          <!-- Sections and Questions -->
          <div v-for="section in currentPage.sections" :key="'cs'+section.id" class="canvas-section">
            <div class="canvas-section-header">
              <button class="canvas-section-toggle" @click="toggleSection('s'+section.id)" :aria-label="expandedNodes['s'+section.id] ? 'Collapse' : 'Expand'">
                {{ expandedNodes['s'+section.id] ? '▾' : '▸' }}
              </button>
              <input v-model="sectionTitles[section.id]" class="canvas-section-title-input" @blur="saveSectionTitle(section.id)" @keyup.enter="($event.target.blur())" placeholder="Section title" />
              <button class="canvas-section-del" @click="deleteSection(section.id)" aria-label="Delete section">×</button>
            </div>
            <p v-if="section.description" class="canvas-section-desc">{{ section.description }}</p>

            <!-- Editable questions -->
            <div v-if="expandedNodes['s'+section.id]" class="canvas-questions">
              <div v-for="(q, qi) in section.questions" :key="'cq'+q.id"
                   class="canvas-question" :class="{ 'canvas-question--selected': selectedQuestion?.id === q.id }"
                   @click="selectQuestion(q)">
                <div class="canvas-q-row">
                  <span class="canvas-q-number">{{ qi + 1 }}</span>
                  <input v-model="questionTitles[q.id]" class="canvas-q-title-input"
                         @blur="saveQuestionTitle(q.id)" @keyup.enter="($event.target.blur())"
                         @focus="selectQuestion(q)" placeholder="Question" />
                  <span v-if="q.required" class="canvas-q-required">*</span>
                  <span :class="['canvas-q-type-badge', 'qtype-' + q.type.toLowerCase()]">{{ q.type.replace('_',' ') }}</span>
                  <button class="canvas-q-del" @click.stop="deleteQuestion(q.id)" aria-label="Delete question">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  </button>
                </div>

                <!-- Inline form input based on type -->
                <div class="canvas-q-input" @click="selectQuestion(q)">
                  <!-- TEXT / DATE -->
                  <input v-if="q.type === 'TEXT'" class="input" placeholder="Text answer — user will type here" />
                  <input v-else-if="q.type === 'DATE'" class="input" type="text" placeholder="YYYY-MM-DD — user picks a date" readonly />
                  <textarea v-else-if="q.type === 'TEXTAREA'" class="input textarea" rows="2" placeholder="Long answer — user will type here"></textarea>

                  <!-- Inline editable options for choice types -->
                  <div v-else-if="hasOptions(q.type)" class="canvas-options" @click.stop>
                    <div v-for="(opt, oi) in getOptionsList(q)" :key="opt.key || oi" class="canvas-option-row">
                      <span :class="q.type === 'MULTI_CHOICE' ? ['canvas-option-marker','checkbox-marker'] : 'canvas-option-marker'"></span>
                      <input class="canvas-option-input" :value="opt.label"
                             @blur="updateOption(q, oi, $event.target.value)"
                             @keyup.enter="updateOption(q, oi, $event.target.value); if (oi === getOptionsList(q).length - 1) { addOption(q); nextTick(() => $el?.nextElementSibling?.querySelector('input')?.focus()) }" />
                      <button class="canvas-option-remove" @click.stop="removeOption(q, oi)" :aria-label="'Remove option ' + (oi+1)">×</button>
                    </div>
                    <div v-if="getOptionsList(q).length === 0" class="canvas-options-hint">No options yet. Click "Add option" below.</div>
                    <button class="canvas-option-add" @click.stop="addOption(q)" type="button">+ Add option</button>
                  </div>

                  <!-- RATING -->
                  <div v-else-if="q.type === 'RATING'" class="canvas-rating" @click.stop>
                    <span v-for="i in getRatingMax(q.options)" :key="i" class="rating-star"
                          :class="{ 'rating-star--active': i <= getRatingValue(q.options) }"
                          @click="setRatingMax(q, i)">★</span>
                    <span class="canvas-options-hint" style="margin-left:8px">{{ getRatingMax(q.options) }} stars</span>
                  </div>

                  <div v-else class="input muted">{{ q.type.replace('_',' ') }} input</div>
                </div>

                <!-- Visibility rule indicator -->
                <div v-if="q.visibilityRules?.length" class="canvas-q-rules">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  {{ q.visibilityRules.length }} rule{{ q.visibilityRules.length > 1 ? 's' : '' }}
                </div>
              </div>
              <div class="canvas-add-q-wrap">
                <button v-if="!showAddQ || showAddQSection !== section.id" class="canvas-add-q" @click="showAddQ = true; showAddQSection = section.id">+ Add Question</button>
                <div v-else class="canvas-add-q-types">
                  <button v-for="qt in QUICK_TYPES" :key="qt.value" class="canvas-add-q-type" @click="addQuestion(section.id, qt.value); showAddQ = false">
                    <span class="canvas-add-q-abbr">{{ qt.abbr }}</span>{{ qt.label }}
                  </button>
                  <button class="canvas-add-q-type canvas-add-q-cancel" @click="showAddQ = false">Cancel</button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="builder-hint">No pages yet. Add a page from the left panel.</div>
      </main>

      <!-- Right: Properties Panel (only when question selected) -->
      <aside class="builder-right">
        <div v-if="!selectedQuestion" class="builder-hint">
          <p>Click a question to edit its properties</p>
        </div>
        <div v-else class="props-panel">
          <h3 class="props-title">Properties — Q{{ getQuestionIndex(selectedQuestion) + 1 }}</h3>

          <div class="form-group">
            <label class="form-label">Type</label>
            <select v-model="editForm.type" class="input" @change="saveQuestionProperties">
              <option v-for="qt in QUESTION_TYPES" :key="qt.value" :value="qt.value">{{ qt.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-checkbox">
              <input v-model="editForm.required" type="checkbox" @change="saveQuestionProperties" />
              Required question
            </label>
          </div>

          <!-- Visibility Rules -->
          <div class="props-section">
            <h4 class="props-subtitle">Visibility Rules</h4>
            <div v-if="!selectedQuestion.visibilityRules?.length" class="props-empty">No rules — question is always visible</div>
            <div v-for="rule in selectedQuestion.visibilityRules" :key="'r'+rule.id" class="rule-row" :class="{ 'rule-row--broken': isRuleBroken(rule) }">
              <span class="rule-text">
                When {{ getQuestionTitle(rule.sourceQuestionId) }} {{ rule.op }} {{ getOptionLabel(rule.sourceQuestionId, rule.value) || rule.value || '' }}
                <span v-if="isRuleBroken(rule)" class="rule-broken-badge" title="Source question options have changed — rule may not match">⚠</span>
              </span>
              <button class="rule-del" @click="deleteRule(rule.id)" aria-label="Delete rule">×</button>
            </div>
            <button v-if="!showRuleForm" class="btn-secondary" style="width:100%;font-size:var(--text-xs);margin-top:8px" @click="openRuleForm">+ Add Rule</button>
            <div v-if="showRuleForm" class="rule-form">
              <label class="rule-step-label">1. Source question</label>
              <select v-model="newRule.sourcePageId" class="input" style="margin-bottom:4px" @change="onCascadePage">
                <option :value="null" disabled>Select page...</option>
                <option v-for="p in availableSourcePages" :key="'rp'+p.id" :value="p.id">{{ p.title }}</option>
              </select>
              <select v-if="newRule.sourcePageId" v-model="newRule.sourceSectionId" class="input" style="margin-bottom:4px" @change="onCascadeSection">
                <option :value="null" disabled>Select section...</option>
                <option v-for="s in availableSourceSections" :key="'rs'+s.id" :value="s.id">{{ s.title }}</option>
              </select>
              <select v-if="newRule.sourceSectionId" v-model="newRule.sourceQuestionId" class="input" style="margin-bottom:6px" @change="onSourceQuestionChange">
                <option :value="null" disabled>Select question...</option>
                <option v-for="q in availableSourceQuestions" :key="'rq'+q.id" :value="q.id">{{ q.title }} ({{ q.type.replace('_',' ') }})</option>
              </select>
              <label class="rule-step-label">2. Condition</label>
              <select v-model="newRule.op" class="input" style="margin-bottom:6px">
                <option v-for="op in availableOperators" :key="op.value" :value="op.value">{{ op.label }}</option>
              </select>
              <select v-if="sourceQuestionHasOptions" v-model="newRule.value" class="input" style="margin-bottom:6px">
                <option :value="null" disabled>Select value...</option>
                <option v-for="opt in sourceQuestionOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <input v-else-if="newRule.op !== 'answered' && newRule.op !== 'not_answered' && newRule.op !== 'is_empty' && newRule.op !== 'not_empty'" v-model="newRule.value" class="input" :placeholder="sourceQuestionType === 'RATING' || sourceQuestionType === 'NUMBER' ? 'Enter number' : 'Value'" style="margin-bottom:6px" />
              <div class="rule-form-btns">
                <button class="btn-secondary" style="flex:1;font-size:var(--text-xs)" @click="closeRuleForm">Cancel</button>
                <button class="btn-primary" style="flex:2;font-size:var(--text-xs)" :disabled="!newRule.sourceQuestionId || !newRule.op" @click="addRule">Add Rule</button>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSurveyStore } from '@/stores/survey'
import { updateTemplateApi, addPageApi, deletePageApi, updatePageApi, addSectionApi, deleteSectionApi, updateSectionApi, addQuestionApi, updateQuestionApi, deleteQuestionApi, addRuleApi, deleteRuleApi } from '@/api/survey'

const store = useSurveyStore()
const route = useRoute()
const router = useRouter()

const template = computed(() => store.currentTemplate)

const currentPageIndex = ref(0)
const currentPage = computed(() => template.value?.pages?.[currentPageIndex.value] || null)
const currentPageTitle = computed({
  get: () => currentPage.value?.title || '',
  set: (val) => { if (currentPage.value) currentPage.value.title = val }
})

const selectedQuestion = ref(null)
const showRuleForm = ref(false)
const showAddQ = ref(false)
const showAddQSection = ref(null)

const QUICK_TYPES = [
  { value: 'TEXT', label: 'Text', abbr: 'T' },
  { value: 'TEXTAREA', label: 'Long Text', abbr: 'L' },
  { value: 'SINGLE_CHOICE', label: 'Choice', abbr: '○' },
  { value: 'MULTI_CHOICE', label: 'Multi-choice', abbr: '☐' },
  { value: 'DROPDOWN', label: 'Dropdown', abbr: '▾' },
  { value: 'RATING', label: 'Rating', abbr: '★' },
  { value: 'DATE', label: 'Date', abbr: 'D' },
  { value: 'TABLE', label: 'Table', abbr: '⊞' },
]

// Reactive maps for inline editing
const sectionTitles = reactive({})
const questionTitles = reactive({})
const expandedNodes = reactive({})
const optionsCache = reactive({}) // questionId → [{key, label}]

const QUESTION_TYPES = [
  { value: 'SINGLE_CHOICE', label: 'Single Choice' },
  { value: 'MULTI_CHOICE', label: 'Multi Choice' },
  { value: 'TEXT', label: 'Text' },
  { value: 'TEXTAREA', label: 'Textarea' },
  { value: 'DATE', label: 'Date' },
  { value: 'DROPDOWN', label: 'Dropdown' },
  { value: 'CASCADER', label: 'Cascader' },
  { value: 'RATING', label: 'Rating' },
  { value: 'TABLE', label: 'Table' },
]

const editForm = reactive({ title: '', type: '', required: false })
const newRule = reactive({ sourcePageId: null, sourceSectionId: null, sourceQuestionId: null, op: 'eq', value: '' })

const allQuestions = computed(() => {
  const qs = []
  if (!template.value?.pages) return qs
  for (const p of template.value.pages) {
    for (const s of p.sections || []) {
      for (const q of s.questions || []) {
        qs.push(q)
      }
    }
  }
  return qs
})

// Questions that come BEFORE the selected question (eligible as rule sources)
const questionsBeforeCurrent = computed(() => {
  if (!selectedQuestion.value) return []
  const before = []
  let found = false
  for (const q of allQuestions.value) {
    if (q.id === selectedQuestion.value.id) { found = true; break }
    before.push(q)
  }
  return before
})

// Cascading source selector: pages with questions before current
const availableSourcePages = computed(() => {
  if (!selectedQuestion.value || !template.value?.pages) return []
  const pages = []
  for (const p of template.value.pages) {
    const hasBefore = p.sections?.some(s => s.questions?.some(q => {
      const idx = allQuestions.value.findIndex(aq => aq.id === q.id)
      const curIdx = allQuestions.value.findIndex(aq => aq.id === selectedQuestion.value.id)
      return idx < curIdx
    }))
    if (hasBefore) pages.push(p)
  }
  return pages
})
const availableSourceSections = computed(() => {
  if (!newRule.sourcePageId || !template.value?.pages) return []
  const page = template.value.pages.find(p => p.id === newRule.sourcePageId)
  if (!page) return []
  const curIdx = allQuestions.value.findIndex(aq => aq.id === selectedQuestion.value?.id)
  return (page.sections || []).filter(s => s.questions?.some(q => {
    const idx = allQuestions.value.findIndex(aq => aq.id === q.id)
    return idx < curIdx
  }))
})
const availableSourceQuestions = computed(() => {
  if (!newRule.sourceSectionId || !template.value?.pages) return []
  const curIdx = allQuestions.value.findIndex(aq => aq.id === selectedQuestion.value?.id)
  for (const p of template.value.pages) {
    for (const s of p.sections || []) {
      if (s.id === newRule.sourceSectionId) {
        return (s.questions || []).filter(q => {
          const idx = allQuestions.value.findIndex(aq => aq.id === q.id)
          return idx < curIdx
        })
      }
    }
  }
  return []
})
function onCascadePage() { newRule.sourceSectionId = null; newRule.sourceQuestionId = null; newRule.op = 'eq'; newRule.value = '' }
function onCascadeSection() { newRule.sourceQuestionId = null; newRule.op = 'eq'; newRule.value = '' }

// Operators filtered by source question type
const availableOperators = computed(() => {
  const t = sourceQuestionType.value
  if (!t) return []
  const common = [{ value: 'answered', label: 'is answered' }, { value: 'not_answered', label: 'is not answered' }]
  if (t === 'SINGLE_CHOICE' || t === 'DROPDOWN') return [...common, { value: 'eq', label: 'equals' }, { value: 'neq', label: 'not equals' }]
  if (t === 'MULTI_CHOICE') return [...common, { value: 'in', label: 'includes' }, { value: 'not_in', label: 'does not include' }]
  if (t === 'TEXT' || t === 'TEXTAREA') return [...common, { value: 'eq', label: 'equals' }, { value: 'neq', label: 'not equals' }, { value: 'contains', label: 'contains' }, { value: 'not_contains', label: 'does not contain' }, { value: 'is_empty', label: 'is empty' }, { value: 'not_empty', label: 'is not empty' }]
  if (t === 'RATING' || t === 'NUMBER') return [...common, { value: 'eq', label: 'equals' }, { value: 'neq', label: 'not equals' }, { value: 'gt', label: 'greater than' }, { value: 'gte', label: '≥' }, { value: 'lt', label: 'less than' }, { value: 'lte', label: '≤' }]
  if (t === 'DATE') return [...common, { value: 'eq', label: 'equals' }, { value: 'neq', label: 'not equals' }, { value: 'gt', label: 'after' }, { value: 'lt', label: 'before' }]
  return [...common, { value: 'eq', label: 'equals' }, { value: 'neq', label: 'not equals' }]
})

const sourceQuestion = computed(() => allQuestions.value.find(q => q.id === newRule.sourceQuestionId))
const sourceQuestionType = computed(() => sourceQuestion.value?.type || '')
const sourceQuestionHasOptions = computed(() => ['SINGLE_CHOICE','MULTI_CHOICE','DROPDOWN'].includes(sourceQuestionType.value))
const sourceQuestionOptions = computed(() => {
  if (!sourceQuestionHasOptions.value) return []
  const opts = parseOptions(sourceQuestion.value?.options)
  return opts.map(o => ({ label: o.label, value: o.key })) // use key as value
})
function openRuleForm() {
  newRule.sourcePageId = null; newRule.sourceSectionId = null; newRule.sourceQuestionId = null; newRule.op = 'eq'; newRule.value = ''
  showRuleForm.value = true
}
function closeRuleForm() {
  newRule.sourcePageId = null; newRule.sourceSectionId = null; newRule.sourceQuestionId = null; newRule.op = 'eq'; newRule.value = ''
  showRuleForm.value = false
}
function onSourceQuestionChange() { newRule.op = 'eq'; newRule.value = '' }

// Initialize reactive maps when template loads
watch(template, (t) => {
  if (!t?.pages) return
  t.pages.forEach(p => {
    expandedNodes['p' + p.id] = true
    p.sections?.forEach(s => {
      expandedNodes['s' + s.id] = true
      sectionTitles[s.id] = s.title
      s.questions?.forEach(q => { questionTitles[q.id] = q.title })
    })
  })
}, { immediate: true, deep: true })

onMounted(async () => {
  const id = route.params.id
  await store.fetchTemplate(id)
})

function switchPage(idx) { currentPageIndex.value = idx; selectedQuestion.value = null }
function toggleSection(key) { expandedNodes[key] = !expandedNodes[key] }

function selectQuestion(q) {
  if (selectedQuestion.value?.id === q.id) return // already selected
  selectedQuestion.value = q
  editForm.title = q.title
  editForm.type = q.type
  editForm.required = q.required
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
function getOptionsList(q) {
  if (!optionsCache[q.id]) {
    optionsCache[q.id] = parseOptions(q.options).map(o => ({ ...o }))
  }
  return optionsCache[q.id]
}
// Get option labels only (for display in select/dropdown)
function getOptionLabels(optionsJson) {
  return parseOptions(optionsJson).map(o => o.label)
}
// Get key by label or key (for backward compat resolution)
function getOptionKeyByLabel(optionsJson, labelOrKey) {
  const opts = parseOptions(optionsJson)
  const found = opts.find(o => o.label === labelOrKey || o.key === labelOrKey)
  return found ? found.key : labelOrKey
}
// Get option label by key (for displaying rule values)
function getOptionLabel(sourceQid, key) {
  const srcQ = allQuestions.value.find(q => q.id === sourceQid)
  if (!srcQ) return key
  const opts = parseOptions(srcQ.options)
  const found = opts.find(o => o.key === key)
  return found ? found.label : key
}
function getQuestionIndex(q) {
  if (!currentPage.value) return 0
  for (const s of currentPage.value.sections || []) {
    const idx = (s.questions || []).findIndex(qq => qq.id === q.id)
    if (idx >= 0) return idx
  }
  return 0
}
function addOption(q) {
  if (!optionsCache[q.id]) optionsCache[q.id] = []
  const key = 'opt_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6)
  optionsCache[q.id].push({ key, label: '' })
}
function updateOption(q, index, value) {
  if (!optionsCache[q.id]) return
  const opt = optionsCache[q.id][index]
  if (opt && opt.label !== value) {
    opt.label = value
    saveOptions(q)
  }
}
function removeOption(q, index) {
  if (!optionsCache[q.id]) return
  optionsCache[q.id].splice(index, 1)
  saveOptions(q)
}
function saveOptions(q) {
  const items = (optionsCache[q.id] || []).filter(o => o.label.trim())
  if (items.length > 0) {
    const labels = items.map(o => o.label.trim().toLowerCase())
    if (new Set(labels).size !== items.length) {
      ElMessage.warning('Options must be unique — duplicate found')
      return
    }
  }
  const newOpts = items.map(o => ({ key: o.key, label: o.label.trim() }))
  // Detect option label changes and warn — keys are stable so rules still work
  const oldOpts = parseOptions(q.options)
  for (const oldO of oldOpts) {
    const newO = newOpts.find(n => n.key === oldO.key)
    if (newO && newO.label !== oldO.label) {
      // Label changed but key is stable — rules based on this key still work
      break
    }
  }
  const newOptionsJson = JSON.stringify({ options: newOpts })
  q.options = newOptionsJson // sync local object so rule form dropdowns update
  updateQuestionApi(q.id, { options: newOptionsJson }).catch(() => {})
}

// Find all visibility rules that reference a given source question
function findDownstreamRules(sourceQid) {
  const rules = []
  for (const q of allQuestions.value) {
    if (q.visibilityRules) {
      for (const r of q.visibilityRules) {
        if (r.sourceQuestionId === sourceQid) rules.push(r)
      }
    }
  }
  return rules
}
async function saveAndReturn() {
  // Save page title
  if (currentPage.value) {
    await updatePageApi(currentPage.value.id, { title: currentPage.value.title })
  }
  // Save all section & question titles + inline options
  for (const s of currentPage.value?.sections || []) {
    if (sectionTitles[s.id] && sectionTitles[s.id] !== s.title) {
      await updateSectionApi(s.id, { title: sectionTitles[s.id] })
    }
    for (const q of s.questions || []) {
      if (questionTitles[q.id] && questionTitles[q.id] !== q.title) {
        await updateQuestionApi(q.id, { title: questionTitles[q.id] })
      }
      // Save inline options for choice/dropdown types
      if (hasOptions(q.type) && optionsCache[q.id]) {
        const items = optionsCache[q.id].filter(o => o.label.trim())
        const newOpts = items.map(o => ({ key: o.key, label: o.label.trim() }))
        if (items.length > 0) {
          const newOptionsJson = JSON.stringify({ options: newOpts })
          q.options = newOptionsJson
          await updateQuestionApi(q.id, { options: newOptionsJson })
        }
      }
    }
  }
  router.push('/admin/surveys')
}
function getRatingMax(optionsJson) {
  try { return JSON.parse(optionsJson || '{}').max || 5 }
  catch { return 5 }
}
function getRatingValue(optionsJson) {
  try { return JSON.parse(optionsJson || '{}').value || 3 }
  catch { return 3 }
}
async function setRatingMax(q, max) {
  await updateQuestionApi(q.id, { options: JSON.stringify({ max, value: max > 2 ? 3 : 1 }) })
  await store.fetchTemplate(template.value.id)
}
function hasOptions(type) { return ['SINGLE_CHOICE','MULTI_CHOICE','DROPDOWN','CASCADER'].includes(type) }
function getQuestionTitle(qid) {
  const q = allQuestions.value.find(q => q.id === qid)
  return q ? q.title : 'Q#' + qid
}
function getQuestionPath(q) {
  if (!template.value?.pages) return q.title
  for (const p of template.value.pages) {
    for (const s of p.sections || []) {
      const found = (s.questions || []).find(qq => qq.id === q.id)
      if (found) return `Page "${p.title}" › ${found.title} (${found.type.replace('_',' ')})`
    }
  }
  return `${q.title} (${q.type.replace('_',' ')})`
}

// ── Page Actions ──

async function savePageTitle() {
  if (!currentPage.value) return
  const newTitle = currentPage.value.title
  await updatePageApi(currentPage.value.id, { title: newTitle })
}

async function addPage() {
  const { data } = await addPageApi(template.value.id, { title: 'Page ' + ((template.value.pages?.length || 0) + 1) })
  if (data.code === 200) {
    ElMessage.success('Page added')
    await store.fetchTemplate(template.value.id)
    currentPageIndex.value = template.value.pages.length - 1
  } else ElMessage.error(data.message)
}

async function deletePage(pageId) {
  try { await ElMessageBox.confirm('Delete this page and all its contents?', 'Delete Page', { type: 'warning' }) }
  catch { return }
  await deletePageApi(pageId)
  ElMessage.success('Page deleted')
  currentPageIndex.value = Math.min(currentPageIndex.value, (template.value.pages?.length || 1) - 1)
  await store.fetchTemplate(template.value.id)
}

// ── Section Actions ──

async function saveSectionTitle(sectionId) {
  const newTitle = sectionTitles[sectionId]
  if (!newTitle) return
  await updateSectionApi(sectionId, { title: newTitle })
}
async function addSection(pageId) {
  const { data } = await addSectionApi(pageId, { title: 'New Section' })
  if (data.code === 200) { ElMessage.success('Section added'); await store.fetchTemplate(template.value.id) }
}
async function deleteSection(sectionId) {
  try { await ElMessageBox.confirm('Delete this section?', 'Delete', { type: 'warning' }) } catch { return }
  await deleteSectionApi(sectionId); await store.fetchTemplate(template.value.id)
}

// ── Question Actions ──

async function saveQuestionTitle(qid) {
  const q = allQuestions.value.find(q => q.id === qid)
  if (q && questionTitles[qid] !== q.title) {
    await updateQuestionApi(qid, { title: questionTitles[qid] })
    await store.fetchTemplate(template.value.id)
  }
}
async function addQuestion(sectionId, type = 'TEXT') {
  const { data } = await addQuestionApi(sectionId, { type, title: type === 'SINGLE_CHOICE' ? 'Choose one' : type === 'MULTI_CHOICE' ? 'Select all that apply' : type === 'RATING' ? 'Rate from 1-5' : 'New Question', required: 0 })
  if (data.code === 200) { ElMessage.success('Question added'); await store.fetchTemplate(template.value.id) }
}
async function saveQuestionProperties() {
  if (!selectedQuestion.value) return
  let options = selectedQuestion.value.options
  if (hasOptions(editForm.type)) {
    const items = (optionsCache[selectedQuestion.value.id] || []).filter(o => o.label.trim())
    const newOpts = items.map(o => ({ key: o.key, label: o.label.trim() }))
    options = JSON.stringify({ options: newOpts })
  }
  await updateQuestionApi(selectedQuestion.value.id, {
    title: editForm.title, type: editForm.type, required: editForm.required ? 1 : 0, options
  })
  await store.fetchTemplate(template.value.id)
  const updated = allQuestions.value.find(q => q.id === selectedQuestion.value.id)
  if (updated) selectQuestion(updated)
}
async function deleteQuestion(qid) {
  await deleteQuestionApi(qid); ElMessage.success('Question deleted')
  selectedQuestion.value = null; await store.fetchTemplate(template.value.id)
}

// ── Visibility Rule Actions ──

// Check if a rule value still exists in the source question's current options (for choice types)
function isRuleBroken(rule) {
  if (rule.op === 'answered' || rule.op === 'not_answered' || rule.op === 'is_empty' || rule.op === 'not_empty') return false
  const srcQ = allQuestions.value.find(q => q.id === rule.sourceQuestionId)
  if (!srcQ || !hasOptions(srcQ.type)) return false
  const opts = parseOptions(srcQ.options)
  if (opts.length === 0) return false
  // For 'in', check each value; for others, check the single value
  const vals = rule.op === 'in' || rule.op === 'not_in' ? (rule.value || '').split(',') : [rule.value]
  return vals.some(key => !opts.some(o => o.key === key))
}

async function addRule() {
  if (!newRule.sourceQuestionId || !selectedQuestion.value) return
  // Contradiction check: same logicGroup, same sourceQuestion, contradictory condition
  const existing = selectedQuestion.value.visibilityRules || []
  for (const r of existing) {
    if (r.sourceQuestionId === newRule.sourceQuestionId && r.logicGroup === (newRule.logicGroup || 0)) {
      const contradicts =
        (r.op === 'eq' && newRule.op === 'neq' && r.value === newRule.value) ||
        (r.op === 'neq' && newRule.op === 'eq' && r.value === newRule.value) ||
        (r.op === 'eq' && newRule.op === 'eq' && r.value !== newRule.value) ||
        (r.op === 'in' && newRule.op === 'not_in' && r.value === newRule.value) ||
        (r.op === 'not_in' && newRule.op === 'in' && r.value === newRule.value) ||
        (r.op === 'answered' && newRule.op === 'not_answered') ||
        (r.op === 'not_answered' && newRule.op === 'answered')
      if (contradicts) {
        ElMessage.warning('This rule contradicts an existing rule in the same group — would always be false')
        return
      }
    }
  }
  const { data } = await addRuleApi(template.value.id, {
    targetType: 'QUESTION', targetId: selectedQuestion.value.id,
    sourceQuestionId: newRule.sourceQuestionId, op: newRule.op,
    value: newRule.value || '', logicGroup: 0
  })
  if (data.code === 200) {
    newRule.sourceQuestionId = null; newRule.op = 'eq'; newRule.value = ''
    showRuleForm.value = false
    await store.fetchTemplate(template.value.id)
    const updated = allQuestions.value.find(q => q.id === selectedQuestion.value.id)
    if (updated) selectQuestion(updated)
  }
}
async function deleteRule(ruleId) {
  await deleteRuleApi(ruleId); await store.fetchTemplate(template.value.id)
  const updated = allQuestions.value.find(q => q.id === selectedQuestion.value?.id)
  if (updated) selectQuestion(updated)
}
</script>

<style scoped>
.builder { display: flex; flex-direction: column; height: calc(100vh - 64px); overflow: hidden; }
.builder-topbar { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md) var(--space-lg); background: var(--color-white); border-bottom: 1px solid var(--color-gray-200); flex-shrink: 0; }
.builder-back { display: flex; align-items: center; gap: 4px; padding: 8px 16px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-secondary); background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; min-height: 44px; }
.builder-back svg { width: 18px; height: 18px; }
.builder-title { font-size: var(--text-lg); font-weight: 600; margin: 0; }
.builder-topbar-spacer { flex: 1; }
.builder-loading { padding: var(--space-2xl); text-align: center; color: var(--color-text-muted); }
.builder-hint { padding: var(--space-2xl); text-align: center; color: var(--color-text-muted); font-size: var(--text-sm); }

.builder-layout { display: flex; flex: 1; overflow: hidden; }

/* Left: Page tabs */
.builder-left { width: 280px; flex-shrink: 0; background: var(--color-gray-50); border-right: 1px solid var(--color-gray-200); overflow-y: auto; padding: var(--space-md); }
.builder-left-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-md); }
.builder-left-title { font-size: var(--text-sm); font-weight: 600; margin: 0; color: var(--color-text-primary); }

/* Page tabs */
.page-tabs { display: flex; flex-direction: column; gap: 2px; }
.page-tab { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: var(--radius-md); cursor: pointer; font-size: var(--text-sm); transition: background var(--transition-fast); min-height: 44px; }
.page-tab:hover { background: var(--color-gray-100); }
.page-tab--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 600; }
.page-tab-num { width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; font-size: var(--text-xs); font-weight: 700; background: var(--color-gray-200); border-radius: 50%; flex-shrink: 0; }
.page-tab--active .page-tab-num { background: var(--color-primary); color: white; }
.page-tab-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.page-tab-del { width: 20px; height: 20px; font-size: 14px; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; display: none; }
.page-tab:hover .page-tab-del { display: flex; align-items: center; justify-content: center; }
.page-tab-del:hover { background: #FEE2E2; color: #B91C1C; }

/* Center: Canvas */
.builder-center { flex: 1; overflow-y: auto; padding: var(--space-xl); background: #F8F9FB; }
.canvas { max-width: 860px; margin: 0 auto; }

/* Canvas page header */
.canvas-page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-2xl); gap: var(--space-md); }
.canvas-page-title-input { flex: 1; font-size: var(--text-2xl); font-weight: 700; font-family: var(--font-heading); border: 1px solid transparent; background: transparent; padding: 8px 12px; border-radius: var(--radius-sm); color: var(--color-text-primary); outline: none; }
.canvas-page-title-input:hover { border-color: var(--color-gray-200); }
.canvas-page-title-input:focus { border-color: var(--color-primary); background: var(--color-white); }
.canvas-add-section { padding: 10px 20px; font-size: var(--text-sm); font-weight: 500; color: var(--color-primary); background: var(--color-primary-bg); border: 1px dashed var(--color-primary); border-radius: var(--radius-md); cursor: pointer; white-space: nowrap; min-height: 44px; }
.canvas-add-section:hover { background: var(--color-primary); color: white; }

/* Section */
.canvas-section { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); margin-bottom: var(--space-lg); }
.canvas-section-header { display: flex; align-items: center; gap: 8px; margin-bottom: var(--space-sm); }
.canvas-section-toggle { width: 32px; height: 32px; font-size: 14px; background: none; border: none; cursor: pointer; color: var(--color-text-muted); border-radius: var(--radius-sm); }
.canvas-section-toggle:hover { background: var(--color-gray-100); }
.canvas-section-title-input { flex: 1; font-size: var(--text-base); font-weight: 600; font-family: var(--font-heading); border: 1px solid transparent; background: transparent; padding: 4px 8px; border-radius: var(--radius-sm); color: var(--color-text-primary); outline: none; min-height: 32px; }
.canvas-section-title-input:hover { border-color: var(--color-gray-200); }
.canvas-section-title-input:focus { border-color: var(--color-primary); background: var(--color-white); }
.canvas-section-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0 0 var(--space-sm); }
.canvas-section-del { width: 32px; height: 32px; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; display: none; }
.canvas-section:hover .canvas-section-del { display: flex; align-items: center; justify-content: center; }
.canvas-section-del:hover { background: #FEE2E2; color: #B91C1C; }

/* Question cards — clear visual separation */
.canvas-questions { margin-top: var(--space-sm); }
.canvas-question {
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-left: 4px solid var(--color-gray-300);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  margin-bottom: var(--space-xl);
  cursor: pointer;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast);
  position: relative;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.canvas-question:hover { border-color: var(--color-primary-light); box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.canvas-question--selected { border-color: var(--color-primary); border-left-color: var(--color-primary); box-shadow: 0 0 0 4px rgba(124,58,237,0.12), 0 4px 12px rgba(0,0,0,0.06); background: #FAFAFE; }
.canvas-q-number { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; color: var(--color-primary); background: var(--color-primary-bg); border-radius: 50%; flex-shrink: 0; margin-right: 2px; }
.canvas-q-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.canvas-q-title-input { flex: 1; font-size: var(--text-sm); font-weight: 500; border: 1px solid transparent; background: transparent; padding: 4px 8px; border-radius: var(--radius-sm); color: var(--color-text-primary); outline: none; }
.canvas-q-title-input:hover { border-color: var(--color-gray-200); }
.canvas-q-title-input:focus { border-color: var(--color-primary); background: var(--color-white); }
.canvas-q-required { color: var(--color-danger); font-weight: 700; font-size: var(--text-sm); }
.canvas-q-type-badge { font-size: 10px; font-weight: 600; color: var(--color-text-muted); background: var(--color-gray-100); padding: 2px 6px; border-radius: var(--radius-full); text-transform: uppercase; white-space: nowrap; }
.qtype-single_choice { background: #DBEAFE; color: #1D4ED8; }
.qtype-multi_choice { background: #D1FAE5; color: #047857; }
.qtype-text { background: var(--color-gray-100); color: var(--color-text-secondary); }
.qtype-textarea { background: var(--color-gray-100); color: var(--color-text-secondary); }
.qtype-date { background: #FEF3C7; color: #92400E; }
.qtype-dropdown { background: #EDE9FE; color: #6D28D9; }
.qtype-cascader { background: #FCE7F3; color: #9D174D; }
.qtype-rating { background: #FFF7ED; color: #C2410C; }
.qtype-table { background: #E0F2FE; color: #0369A1; }
.canvas-q-del { width: 32px; height: 32px; min-width: 32px; padding: 0; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; display: none; }
.canvas-q-del svg { width: 14px; height: 14px; }
.canvas-question:hover .canvas-q-del { display: flex; align-items: center; justify-content: center; }
.canvas-q-del:hover { background: #FEE2E2; color: #B91C1C; }
.canvas-q-input { padding-left: 0; }
.canvas-q-rules { margin-top: 8px; padding-top: 6px; border-top: 1px dashed var(--color-gray-200); font-size: 10px; color: var(--color-primary); display: flex; align-items: center; gap: 4px; }
.canvas-add-q { width: 100%; padding: 12px; font-size: var(--text-sm); color: var(--color-primary); background: none; border: 1px dashed var(--color-gray-300); border-radius: var(--radius-md); cursor: pointer; margin-top: var(--space-xs); min-height: 44px; }
.canvas-add-q:hover { border-color: var(--color-primary); background: var(--color-primary-bg); }

.canvas-add-q-types { display: flex; flex-wrap: wrap; gap: 6px; margin-top: var(--space-xs); }
.canvas-add-q-type { display: flex; align-items: center; gap: 4px; padding: 8px 12px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); color: var(--color-text-primary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; min-height: 36px; transition: all var(--transition-fast); }
.canvas-add-q-type:hover { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-bg); }
.canvas-add-q-abbr { width: 18px; height: 18px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; border-radius: 3px; background: var(--color-gray-100); color: var(--color-text-secondary); flex-shrink: 0; }
.canvas-add-q-cancel { color: var(--color-text-muted); border-style: dashed; }
.canvas-add-q-cancel:hover { color: var(--color-text-secondary); border-color: var(--color-gray-400); background: var(--color-gray-50); }
.canvas-add-q-wrap { margin-top: var(--space-xs); }

/* Inline editable options (for choice/dropdown types) */
.canvas-options { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; }
.canvas-option-row { display: flex; align-items: center; gap: 10px; padding: 4px 0; }
.canvas-option-marker { width: 16px; height: 16px; border: 2px solid var(--color-gray-300); border-radius: 50%; flex-shrink: 0; }
.checkbox-marker { border-radius: 3px; }
.canvas-option-input { flex: 1; font-size: var(--text-sm); padding: 8px 10px; border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm); outline: none; color: var(--color-text-primary); }
.canvas-option-input:focus { border-color: var(--color-primary); }
.canvas-option-remove { width: 32px; height: 32px; padding: 0; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; font-size: 18px; }
.canvas-option-remove:hover { background: #FEE2E2; color: #B91C1C; }
.canvas-option-add { padding: 8px 12px; font-size: var(--text-xs); color: var(--color-primary); background: none; border: none; cursor: pointer; margin-top: 2px; min-height: 36px; }
.canvas-option-add:hover { text-decoration: underline; }
.canvas-options-hint { font-size: var(--text-xs); color: var(--color-text-muted); font-style: italic; margin-top: 4px; }
.canvas-rating { display: flex; gap: 4px; margin-top: 4px; }
.rating-star { font-size: 22px; color: var(--color-gray-300); cursor: pointer; }
.rating-star--active { color: #F59E0B; }

/* Right: Properties */
.builder-right { width: 320px; flex-shrink: 0; background: var(--color-gray-50); border-left: 1px solid var(--color-gray-200); overflow-y: auto; padding: var(--space-lg); }
.props-panel { display: flex; flex-direction: column; gap: var(--space-lg); }
.props-title { font-size: var(--text-base); font-weight: 600; margin: 0 0 var(--space-sm); }
.props-subtitle { font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); margin: var(--space-md) 0 var(--space-sm); border-top: 1px solid var(--color-gray-200); padding-top: var(--space-md); }
.props-section { margin-top: var(--space-sm); }
.props-empty { font-size: var(--text-xs); color: var(--color-text-muted); padding: var(--space-sm) 0; }

.rule-row { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm); margin-bottom: 4px; font-size: var(--text-xs); min-height: 36px; }
.rule-row--broken { border-color: #FCD34D; background: #FFFBEB; }
.rule-text { flex: 1; color: var(--color-text-secondary); display: flex; align-items: center; gap: 4px; }
.rule-broken-badge { font-size: 11px; cursor: help; }
.rule-del { width: 24px; height: 24px; padding: 0; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; }
.rule-form { margin-top: var(--space-sm); }
.rule-step-label { display: block; font-size: 10px; font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 2px; }
.rule-form-btns { display: flex; gap: 6px; margin-top: 4px; }

/* Shared */
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.icon-btn { width: 28px; height: 28px; padding: 0; font-size: 18px; font-weight: 600; color: var(--color-primary); background: var(--color-primary-bg); border: none; border-radius: var(--radius-sm); cursor: pointer; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-label { font-size: var(--text-xs); font-weight: 500; color: var(--color-text-secondary); }
.form-checkbox { display: flex; align-items: center; gap: 8px; font-size: var(--text-sm); color: var(--color-text-secondary); cursor: pointer; }
.form-checkbox input { width: 16px; height: 16px; cursor: pointer; }
.required { color: var(--color-danger); }
.input { padding: 8px 10px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.textarea { resize: vertical; }
.muted { color: var(--color-text-muted) !important; }
.btn-primary { padding: 10px 20px; font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: var(--color-primary); border: none; border-radius: var(--radius-md); cursor: pointer; font-size: var(--text-sm); min-height: 44px; }
.btn-primary:hover:not(:disabled) { opacity: 0.9; }
.btn-secondary { padding: 10px 20px; font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; font-size: var(--text-sm); min-height: 44px; }
.btn-secondary:hover { background: var(--color-gray-50); }

@media (max-width: 1024px) {
  .builder-left { width: 220px; }
  .builder-right { width: 260px; }
}
@media (max-width: 768px) {
  .builder-layout { flex-direction: column; }
  .builder-left, .builder-right { width: 100%; max-height: 200px; }
}
</style>
