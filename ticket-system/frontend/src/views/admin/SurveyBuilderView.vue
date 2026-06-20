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
      <button class="btn-secondary" @click="saveAndReturn">Save &amp; Return</button>
    </div>

    <div v-if="!template" class="builder-loading">Loading template...</div>

    <!-- 3-column layout -->
    <div v-else class="builder-layout">
      <!-- Left: Structure Tree -->
      <aside class="builder-left">
        <div class="builder-left-header">
          <h3 class="builder-left-title">Structure</h3>
          <button class="icon-btn" title="Add Page" @click="addPage" aria-label="Add page">+</button>
        </div>
        <div class="tree">
          <div v-for="(page, pi) in template.pages" :key="'p'+page.id" class="tree-node">
            <div class="tree-row" :class="{ 'tree-row--active': selectedPage?.id === page.id }" @click="selectPage(page)">
              <span class="tree-toggle" @click.stop="toggleNode('p'+page.id)">{{ expandedNodes['p'+page.id] ? '&#9662;' : '&#9656;' }}</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="tree-icon" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14,2 14,8 20,8"/></svg>
              <span class="tree-label">{{ page.title }}</span>
              <button class="tree-del" @click.stop="deletePage(page.id)" :disabled="template.pages.length <= 1" aria-label="Delete page">&times;</button>
            </div>
            <div v-if="expandedNodes['p'+page.id]" class="tree-children">
              <div v-for="(section, si) in page.sections" :key="'s'+section.id" class="tree-node">
                <div class="tree-row" :class="{ 'tree-row--active': selectedSection?.id === section.id }" @click="selectSection(section)">
                  <span class="tree-toggle" @click.stop="toggleNode('s'+section.id)">{{ expandedNodes['s'+section.id] ? '&#9662;' : '&#9656;' }}</span>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="tree-icon" aria-hidden="true"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
                  <span class="tree-label">{{ section.title }}</span>
                  <button class="tree-del" @click.stop="deleteSection(section.id)" aria-label="Delete section">&times;</button>
                </div>
                <div v-if="expandedNodes['s'+section.id]" class="tree-children">
                  <div v-for="(q, qi) in section.questions" :key="'q'+q.id" class="tree-row tree-row--q" :class="{ 'tree-row--active': selectedQuestion?.id === q.id }" @click="selectQuestion(q)">
                    <span class="tree-q-type">{{ q.type.replace('_',' ').substring(0,6) }}</span>
                    <span class="tree-label tree-label--q">{{ q.title }}</span>
                    <button class="tree-del" @click.stop="deleteQuestion(q.id)" aria-label="Delete question">&times;</button>
                  </div>
                  <button class="tree-add-btn" @click="addQuestion(section.id)">+ Question</button>
                </div>
              </div>
              <button class="tree-add-btn" @click="addSection(page.id)">+ Section</button>
            </div>
          </div>
        </div>
      </aside>

      <!-- Center: Canvas -->
      <main class="builder-center">
        <div v-if="!selectedPage &amp;&amp; !selectedSection" class="builder-hint">Select a page or section to preview</div>

        <!-- Page preview -->
        <div v-if="selectedPage" class="canvas-page">
          <h2 class="canvas-page-title">{{ selectedPage.title }}</h2>
          <div v-for="section in selectedPage.sections" :key="'cs'+section.id" class="canvas-section">
            <h3 class="canvas-section-title">{{ section.title }}</h3>
            <p v-if="section.description" class="canvas-section-desc">{{ section.description }}</p>
            <div v-for="q in section.questions" :key="'cq'+q.id" class="canvas-question" @click="selectQuestion(q)">
              <label class="canvas-q-label">{{ q.title }} <span v-if="q.required" class="required">*</span></label>
              <!-- Render based on type -->
              <input v-if="q.type === 'TEXT' || q.type === 'DATE'" :type="q.type === 'DATE' ? 'date' : 'text'" class="input" disabled :placeholder="'Enter ' + q.title.toLowerCase()" />
              <textarea v-else-if="q.type === 'TEXTAREA'" class="input textarea" disabled rows="2" :placeholder="'Enter ' + q.title.toLowerCase()"></textarea>
              <div v-else-if="q.type === 'SINGLE_CHOICE' || q.type === 'MULTI_CHOICE'" class="canvas-options">
                <label v-for="(opt, oi) in parseOptions(q.options)" :key="oi" class="canvas-opt">
                  <span :class="q.type === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'"></span> {{ opt }}
                </label>
              </div>
              <select v-else-if="q.type === 'DROPDOWN'" class="input" disabled><option>{{ parseOptions(q.options)[0] || 'Select...' }}</option></select>
              <div v-else-if="q.type === 'RATING'" class="canvas-rating">
                <span v-for="i in getRatingMax(q.options)" :key="i" class="rating-star">&#9733;</span>
              </div>
              <div v-else-if="q.type === 'CASCADER'" class="input" style="color:var(--color-text-muted)">Cascader ({{ q.title }})</div>
              <div v-else-if="q.type === 'TABLE'" class="input" style="color:var(--color-text-muted)">Table input</div>
              <input v-else class="input" disabled :placeholder="'Enter ' + q.title.toLowerCase()" />
            </div>
          </div>
        </div>
      </main>

      <!-- Right: Properties -->
      <aside class="builder-right">
        <div v-if="!selectedQuestion" class="builder-hint">Select a question to edit properties</div>
        <div v-else class="props-panel">
          <h3 class="props-title">Question Properties</h3>
          <div class="form-group">
            <label class="form-label">Title</label>
            <input v-model="editForm.title" class="input" @change="saveQuestion" />
          </div>
          <div class="form-group">
            <label class="form-label">Type</label>
            <select v-model="editForm.type" class="input" @change="saveQuestion">
              <option v-for="qt in QUESTION_TYPES" :key="qt.value" :value="qt.value">{{ qt.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-checkbox">
              <input v-model="editForm.required" type="checkbox" @change="saveQuestion" />
              Required
            </label>
          </div>
          <div v-if="hasOptions(editForm.type)" class="form-group">
            <label class="form-label">Options (one per line)</label>
            <textarea v-model="editForm.optionsText" class="input textarea" rows="4" @change="onOptionsChange" placeholder="Option 1&#10;Option 2&#10;Option 3"></textarea>
          </div>

          <!-- Visibility Rules -->
          <div class="props-section">
            <h4 class="props-subtitle">Visibility Rules</h4>
            <div v-for="rule in selectedQuestion.visibilityRules" :key="'r'+rule.id" class="rule-row">
              <span class="rule-text">When {{ getQuestionTitle(rule.sourceQuestionId) }} {{ rule.op }} {{ rule.value || '' }}</span>
              <button class="tree-del" @click="deleteRule(rule.id)" aria-label="Delete rule">&times;</button>
            </div>
            <div v-if="showRuleForm" class="rule-form">
              <select v-model="newRule.sourceQuestionId" class="input" style="margin-bottom:6px">
                <option :value="null" disabled>Select source question...</option>
                <option v-for="q in allQuestions" :key="'sq'+q.id" :value="q.id">{{ q.title }}</option>
              </select>
              <select v-model="newRule.op" class="input" style="margin-bottom:6px">
                <option value="eq">equals</option>
                <option value="neq">not equals</option>
                <option value="answered">is answered</option>
                <option value="not_answered">is not answered</option>
                <option value="contains">contains</option>
                <option value="in">in</option>
                <option value="gt">greater than</option>
                <option value="lt">less than</option>
              </select>
              <input v-if="newRule.op !== 'answered' &amp;&amp; newRule.op !== 'not_answered'" v-model="newRule.value" class="input" placeholder="Value" style="margin-bottom:6px" />
              <button class="btn-primary" style="width:100%;font-size:var(--text-xs)" @click="addRule">Add Rule</button>
            </div>
            <button v-if="!showRuleForm" class="btn-secondary" style="width:100%;font-size:var(--text-xs)" @click="showRuleForm = true">+ Add Visibility Rule</button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSurveyStore } from '@/stores/survey'
import { addPageApi, deletePageApi, addSectionApi, deleteSectionApi, addQuestionApi, updateQuestionApi, deleteQuestionApi, addRuleApi, deleteRuleApi } from '@/api/survey'

const store = useSurveyStore()
const route = useRoute()
const router = useRouter()

const template = computed(() => store.currentTemplate)
const selectedPage = ref(null)
const selectedSection = ref(null)
const selectedQuestion = ref(null)
const expandedNodes = reactive({})
const showRuleForm = ref(false)

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

const editForm = reactive({ title: '', type: '', required: false, optionsText: '' })
const newRule = reactive({ sourceQuestionId: null, op: 'eq', value: '' })

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

onMounted(async () => {
  const id = route.params.id
  await store.fetchTemplate(id)
  if (template.value?.pages?.length > 0) {
    expandedNodes['p' + template.value.pages[0].id] = true
    selectPage(template.value.pages[0])
  }
})

function toggleNode(key) { expandedNodes[key] = !expandedNodes[key] }

function selectPage(page) {
  selectedPage.value = page
  selectedSection.value = null
  selectedQuestion.value = null
}
function selectSection(section) {
  selectedSection.value = section
  selectedPage.value = null
  selectedQuestion.value = null
}
function selectQuestion(q) {
  selectedQuestion.value = q
  editForm.title = q.title
  editForm.type = q.type
  editForm.required = q.required
  editForm.optionsText = parseOptions(q.options).join('\n')
}

function parseOptions(optionsJson) {
  try { const o = JSON.parse(optionsJson || '{}'); return o.options || [] }
  catch { return [] }
}
function getRatingMax(optionsJson) {
  try { return JSON.parse(optionsJson || '{}').max || 5 }
  catch { return 5 }
}
function hasOptions(type) { return ['SINGLE_CHOICE','MULTI_CHOICE','DROPDOWN','CASCADER'].includes(type) }
function getQuestionTitle(qid) {
  const q = allQuestions.value.find(q => q.id === qid)
  return q ? q.title : 'Q#' + qid
}

// ── Actions ──

async function addPage() {
  const { data } = await addPageApi(template.value.id, { title: 'Page ' + ((template.value.pages?.length || 0) + 1) })
  if (data.code === 200) {
    ElMessage.success('Page added')
    await store.fetchTemplate(template.value.id)
    expandedNodes['p' + data.data.id] = true
    selectPage(data.data)
  } else ElMessage.error(data.message)
}

async function deletePage(pageId) {
  try { await ElMessageBox.confirm('Delete this page and all its contents?', 'Delete Page', { type: 'warning' }) }
  catch { return }
  await deletePageApi(pageId)
  ElMessage.success('Page deleted')
  selectedPage.value = null
  await store.fetchTemplate(template.value.id)
}

async function addSection(pageId) {
  const { data } = await addSectionApi(pageId, { title: 'New Section' })
  if (data.code === 200) ElMessage.success('Section added'); await store.fetchTemplate(template.value.id)
}

async function deleteSection(sectionId) {
  try { await ElMessageBox.confirm('Delete this section?', 'Delete', { type: 'warning' }) } catch { return }
  await deleteSectionApi(sectionId); await store.fetchTemplate(template.value.id)
}

async function addQuestion(sectionId) {
  const { data } = await addQuestionApi(sectionId, { type: 'TEXT', title: 'New Question', required: 0 })
  if (data.code === 200) { ElMessage.success('Question added'); await store.fetchTemplate(template.value.id) }
}

async function saveQuestion() {
  if (!selectedQuestion.value) return
  let options = selectedQuestion.value.options
  if (hasOptions(editForm.type)) {
    const items = editForm.optionsText.split('\n').map(s => s.trim()).filter(Boolean)
    options = JSON.stringify({ options: items })
  }
  await updateQuestionApi(selectedQuestion.value.id, {
    title: editForm.title, type: editForm.type, required: editForm.required ? 1 : 0, options
  })
  await store.fetchTemplate(template.value.id)
  // re-select the question
  const updated = allQuestions.value.find(q => q.id === selectedQuestion.value.id)
  if (updated) selectQuestion(updated)
}

function onOptionsChange() { saveQuestion() }

async function deleteQuestion(qid) {
  await deleteQuestionApi(qid); ElMessage.success('Question deleted')
  selectedQuestion.value = null; await store.fetchTemplate(template.value.id)
}

async function addRule() {
  if (!newRule.sourceQuestionId || !selectedQuestion.value) return
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

function saveAndReturn() { router.push('/admin/surveys') }
</script>

<style scoped>
.builder { display: flex; flex-direction: column; height: calc(100vh - 64px); overflow: hidden; }
.builder-topbar { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md) var(--space-lg); background: var(--color-white); border-bottom: 1px solid var(--color-gray-200); flex-shrink: 0; }
.builder-back { display: flex; align-items: center; gap: 4px; padding: 6px 12px; font-size: var(--text-sm); font-family: var(--font-body); color: var(--color-text-secondary); background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.builder-back svg { width: 16px; height: 16px; }
.builder-title { font-size: var(--text-lg); font-weight: 600; margin: 0; }
.builder-topbar-spacer { flex: 1; }
.builder-loading { padding: var(--space-2xl); text-align: center; color: var(--color-text-muted); }
.builder-hint { padding: var(--space-2xl); text-align: center; color: var(--color-text-muted); font-size: var(--text-sm); }

.builder-layout { display: flex; flex: 1; overflow: hidden; }

/* Left: Tree */
.builder-left { width: 280px; flex-shrink: 0; background: var(--color-gray-50); border-right: 1px solid var(--color-gray-200); overflow-y: auto; padding: var(--space-md); }
.builder-left-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-md); }
.builder-left-title { font-size: var(--text-sm); font-weight: 600; margin: 0; color: var(--color-text-primary); }

.tree-node { margin-bottom: 2px; }
.tree-row { display: flex; align-items: center; gap: 4px; padding: 6px 8px; border-radius: var(--radius-sm); cursor: pointer; font-size: var(--text-sm); transition: background var(--transition-fast); }
.tree-row:hover { background: var(--color-gray-100); }
.tree-row--active { background: var(--color-primary-bg); color: var(--color-primary); }
.tree-row--q { padding-left: 32px; }
.tree-toggle { font-size: 10px; width: 14px; color: var(--color-text-muted); flex-shrink: 0; }
.tree-icon { width: 14px; height: 14px; flex-shrink: 0; color: var(--color-text-muted); }
.tree-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-label--q { font-size: var(--text-xs); }
.tree-q-type { font-size: 9px; font-weight: 600; text-transform: uppercase; color: var(--color-text-muted); background: var(--color-gray-200); padding: 1px 4px; border-radius: 3px; }
.tree-del { width: 20px; height: 20px; padding: 0; font-size: 14px; line-height: 1; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; display: none; }
.tree-row:hover .tree-del { display: inline-flex; align-items: center; justify-content: center; }
.tree-del:hover { background: #FEE2E2; color: #B91C1C; }
.tree-children { margin-left: 16px; }
.tree-add-btn { width: 100%; padding: 4px 8px; font-size: var(--text-xs); color: var(--color-primary); background: none; border: 1px dashed var(--color-gray-300); border-radius: var(--radius-sm); cursor: pointer; margin-top: 2px; }
.tree-add-btn:hover { border-color: var(--color-primary); background: var(--color-primary-bg); }

/* Center: Canvas */
.builder-center { flex: 1; overflow-y: auto; padding: var(--space-lg); background: var(--color-gray-50); }
.canvas-page { max-width: 640px; margin: 0 auto; }
.canvas-page-title { font-size: var(--text-xl); font-weight: 700; margin: 0 0 var(--space-lg); }
.canvas-section { background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-lg); padding: var(--space-lg); margin-bottom: var(--space-lg); }
.canvas-section-title { font-size: var(--text-base); font-weight: 600; margin: 0 0 var(--space-xs); }
.canvas-section-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0 0 var(--space-md); }
.canvas-question { margin-bottom: var(--space-md); padding: var(--space-sm); border-radius: var(--radius-md); cursor: pointer; transition: background var(--transition-fast); }
.canvas-question:hover { background: var(--color-primary-bg); }
.canvas-q-label { display: block; font-size: var(--text-sm); font-weight: 500; margin-bottom: 6px; }
.canvas-options { display: flex; flex-direction: column; gap: 6px; }
.canvas-opt { display: flex; align-items: center; gap: 8px; font-size: var(--text-sm); color: var(--color-text-secondary); }
.radio, .checkbox { width: 16px; height: 16px; border: 2px solid var(--color-gray-300); border-radius: 50%; }
.checkbox { border-radius: 3px; }
.canvas-rating { display: flex; gap: 4px; }
.rating-star { font-size: 24px; color: var(--color-gray-300); }

/* Right: Properties */
.builder-right { width: 300px; flex-shrink: 0; background: var(--color-gray-50); border-left: 1px solid var(--color-gray-200); overflow-y: auto; padding: var(--space-md); }
.props-panel { display: flex; flex-direction: column; gap: var(--space-md); }
.props-title { font-size: var(--text-sm); font-weight: 600; margin: 0; }
.props-subtitle { font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); margin: var(--space-md) 0 var(--space-sm); border-top: 1px solid var(--color-gray-200); padding-top: var(--space-md); }
.props-section { margin-top: var(--space-sm); }

.rule-row { display: flex; align-items: center; justify-content: space-between; padding: 6px 8px; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm); margin-bottom: 4px; font-size: var(--text-xs); }
.rule-text { flex: 1; color: var(--color-text-secondary); }
.rule-form { margin-top: var(--space-sm); }

/* Shared */
.status-badge { font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: var(--radius-full); }
.status-draft { background: #D1FAE5; color: #047857; }
.status-published { background: #DBEAFE; color: #1D4ED8; }
.icon-btn { width: 28px; height: 28px; padding: 0; font-size: 18px; font-weight: 600; color: var(--color-primary); background: var(--color-primary-bg); border: none; border-radius: var(--radius-sm); cursor: pointer; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: var(--text-xs); font-weight: 500; color: var(--color-text-secondary); }
.form-checkbox { display: flex; align-items: center; gap: 8px; font-size: var(--text-sm); color: var(--color-text-secondary); cursor: pointer; }
.form-checkbox input { width: 16px; height: 16px; cursor: pointer; }
.required { color: var(--color-danger); }
.input { padding: 8px 10px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }
.textarea { resize: vertical; }
.btn-primary { padding: 8px 16px; font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: var(--color-primary); border: none; border-radius: var(--radius-md); cursor: pointer; }
.btn-primary:hover:not(:disabled) { opacity: 0.9; }
.btn-secondary { padding: 8px 16px; font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
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
