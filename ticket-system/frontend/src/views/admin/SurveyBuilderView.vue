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
      <button v-if="template?.status === 'DRAFT'" class="btn-publish" @click="publishFromBuilder">Publish</button>
      <button class="btn-secondary" @click="saveAndReturn">Done</button>
    </div>

    <div v-if="!template" class="builder-loading">Loading template...</div>

    <div v-else class="builder-layout">
      <!-- Center: Editable Form Canvas -->
      <main class="builder-center">
        <!-- Page tabs at top -->
        <div class="canvas-page-tabs">
          <button v-for="(page, pi) in template.pages" :key="'pt'+page.id"
                  class="page-tab" :class="{ 'page-tab--active': currentPageIndex === pi }"
                  @click="switchPage(pi)">
            <span class="page-tab-num">{{ pi + 1 }}</span>
            <span v-if="editingTab !== 'p'+page.id" class="page-tab-label" @dblclick.stop="editingTab = 'p'+page.id">{{ page.title }}</span>
            <input v-else :value="page.title" class="page-tab-title-input"
                   @click.stop @blur="updatePageTitle(page, $event.target.value); editingTab = null"
                   @keyup.enter="($event.target.blur())" @vue:mounted="$el.focus()" placeholder="Page" />
            <button v-if="template.pages.length > 1" class="page-tab-del" @click.stop="deletePage(page.id)" aria-label="Delete page">×</button>
          </button>
          <button class="page-tab page-tab--add" @click="addPage" aria-label="Add page">+ Add Page</button>
        </div>
        <div v-if="currentPage" class="canvas">
          <!-- Section tabs -->
          <div v-if="currentPage.sections?.length" class="canvas-section-tabs">
            <button v-for="(section, si) in currentPage.sections" :key="'st'+section.id"
                    class="section-tab" :class="{ 'section-tab--active': currentSectionIndex === si }"
                    @click="currentSectionIndex = si">
              <span class="section-tab-num">{{ si + 1 }}</span>
              <span v-if="editingTab !== 's'+section.id" class="section-tab-label" @dblclick.stop="editingTab = 's'+section.id">{{ section.title }}</span>
              <input v-else v-model="sectionTitles[section.id]" class="section-tab-title-input"
                     @click.stop @blur="saveSectionTitle(section.id); editingTab = null"
                     @keyup.enter="($event.target.blur())" @vue:mounted="$el.focus()" placeholder="Section" />
              <span class="section-tab-qs">{{ section.questions?.length || 0 }} Q</span>
              <button v-if="currentPage.sections.length > 1" class="section-tab-del" @click.stop="deleteSection(section.id)" aria-label="Delete section">×</button>
            </button>
            <button class="section-tab section-tab--add" @click="addSection(currentPage.id)" aria-label="Add section">+ Add Section</button>
            <span class="canvas-tab-actions">
              <button class="inline-rules-btn" :class="{ 'inline-rules-btn--empty': countRulesFor('PAGE', currentPage.id) === 0 }" @click="openRuleFor('PAGE', currentPage.id)" :aria-label="'Page visibility rules'" :title="'Page visibility rules'">
                {{ countRulesFor('PAGE', currentPage.id) || 'No' }} page rule{{ countRulesFor('PAGE', currentPage.id) !== 1 ? 's' : '' }}
              </button>
            </span>
          </div>

          <!-- Current Section Content -->
          <div v-if="currentSection" class="canvas">
            <div class="canvas-section-actions">
              <button class="inline-rules-btn" :class="{ 'inline-rules-btn--empty': countRulesFor('SECTION', currentSection.id) === 0 }" @click.stop="openRuleFor('SECTION', currentSection.id)" :aria-label="'Section visibility rules'" :title="'Section visibility rules'">
                {{ countRulesFor('SECTION', currentSection.id) || 'No' }} section rule{{ countRulesFor('SECTION', currentSection.id) !== 1 ? 's' : '' }}
              </button>
            </div>
            <p v-if="currentSection.description" class="canvas-section-desc">{{ currentSection.description }}</p>

            <!-- Editable questions -->
            <div class="canvas-questions">
              <div v-for="(q, qi) in currentSection.questions" :key="'cq'+q.id"
                   class="canvas-question" :class="{ 'canvas-question--selected': selectedQuestion?.id === q.id, 'canvas-question--error': getQuestionErrors(q).length > 0 }"
                   @click="selectQuestion(q)">
                <div class="canvas-q-row">
                  <span class="canvas-q-number">{{ qi + 1 }}</span>
                  <input v-model="questionTitles[q.id]" class="canvas-q-title-input"
                         @blur="saveQuestionTitle(q.id)" @keyup.enter="($event.target.blur())"
                         @focus="selectQuestion(q)" placeholder="Question" />
                  <span v-if="q.required" class="canvas-q-required">*</span>
                  <span :class="['canvas-q-type-badge', 'qtype-' + q.type.toLowerCase()]">{{ q.type.replace('_',' ') }}</span>
                  <button v-if="qi > 0" class="canvas-q-arrow" @click.stop="moveQuestion(currentSection, qi, -1)" aria-label="Move up">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="18,15 12,9 6,15"/></svg>
                  </button>
                  <button v-if="qi < currentSection.questions.length - 1" class="canvas-q-arrow" @click.stop="moveQuestion(currentSection, qi, 1)" aria-label="Move down">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6,9 12,15 18,9"/></svg>
                  </button>
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

                  <table v-else-if="q.type === 'TABLE'" class="canvas-table">
                    <thead><tr><th v-for="col in parseTableColumns(q.options)" :key="col.key">{{ col.label }}</th></tr></thead>
                    <tbody><tr v-for="r in parseTableRows(q.options)" :key="r"><td v-for="col in parseTableColumns(q.options)" :key="col.key">—</td></tr></tbody>
                  </table>
                  <div v-else class="input muted">{{ q.type.replace('_',' ') }} input</div>
                </div>

                <!-- Visibility rule indicator -->
                <div v-if="q.visibilityRules?.length" class="canvas-q-rules">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  {{ q.visibilityRules.length }} rule{{ q.visibilityRules.length > 1 ? 's' : '' }}
                </div>
                <!-- Validation errors -->
                <div v-if="getQuestionErrors(q).length" class="canvas-q-errors">{{ getQuestionErrors(q).join('; ') }}</div>
              </div>
              <div class="canvas-add-q-wrap">
                <button v-if="!showAddQ || showAddQSection !== currentSection.id" class="canvas-add-q" @click="showAddQ = true; showAddQSection = currentSection.id">+ Add Question</button>
                <div v-else class="canvas-add-q-types">
                  <button v-for="qt in QUICK_TYPES" :key="qt.value" class="canvas-add-q-type" @click="addQuestion(currentSection.id, qt.value); showAddQ = false">
                    <span class="canvas-add-q-abbr">{{ qt.abbr }}</span>{{ qt.label }}
                  </button>
                  <button class="canvas-add-q-type canvas-add-q-cancel" @click="showAddQ = false">Cancel</button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="builder-hint">No pages yet. Add a page from the left panel.</div>
        <div v-if="currentPage" class="canvas-scroll-btns">
          <button class="scroll-btn" @click="scrollCanvas('top')" aria-label="Scroll to top">↑ Top</button>
          <button class="scroll-btn" @click="scrollCanvas('bottom')" aria-label="Scroll to bottom">↓ Bottom</button>
        </div>
      </main>

      <!-- Right: Properties Panel -->
      <aside class="builder-right">
        <div v-if="!selectedQuestion && !isShowingNonQTarget" class="builder-hint">
          <p>Click a question to edit its properties</p>
        </div>
        <div v-else-if="isShowingNonQTarget && !selectedQuestion" class="props-panel">
          <h3 class="props-title">Properties — {{ ruleTarget.type === 'PAGE' ? 'Page' : 'Section' }}</h3>

          <!-- Visibility Rules for Page/Section -->
          <div class="props-section">
            <h4 class="props-subtitle">Visibility Rules</h4>
            <div v-if="!currentTargetRules.length" class="props-empty">No rules — {{ ruleTarget.type === 'PAGE' ? 'page' : 'section' }} is always visible</div>
            <template v-for="(grp, gi) in groupedCurrentTargetRules" :key="'g'+grp.group">
              <div class="rule-group-label">{{ gi === 0 ? 'AND — all must match' : 'OR — any can match' }}</div>
              <div v-for="rule in grp.rules" :key="'r'+rule.id" class="rule-row" :class="{ 'rule-row--broken': isRuleBroken(rule) }" @click="editRule(rule)">
                <span class="rule-text">
                  When {{ getQuestionTitle(rule.sourceQuestionId) }} {{ rule.op }} {{ getOptionLabel(rule.sourceQuestionId, rule.value) || rule.value || '' }}
                  <span v-if="isRuleBroken(rule)" class="rule-broken-badge" title="Source question options have changed — rule may not match">⚠</span>
                </span>
                <button class="rule-del" @click.stop="deleteRule(rule.id)" aria-label="Delete rule">×</button>
              </div>
            </template>
            <button v-if="!showRuleForm && canAddRuleForTarget" class="btn-secondary" style="width:100%;font-size:var(--text-xs);margin-top:8px" @click="openRuleForm">+ Add Rule</button>
            <div v-if="showRuleForm" class="rule-form">
              <div class="rule-step">
                <label class="rule-step-label">1. Source question</label>
                <select v-if="availableSourcePagesForTarget.length > 0" v-model="newRule.sourcePageId" class="input rule-cascade-select" @change="onCascadePage">
                  <option :value="null" disabled>Select page...</option>
                  <option v-for="p in availableSourcePagesForTarget" :key="'rp'+p.id" :value="p.id">{{ p.title }}</option>
                </select>
                <div v-else class="rule-empty-hint">No earlier questions exist to use as conditions</div>
                <select v-if="newRule.sourcePageId" v-model="newRule.sourceSectionId" class="input rule-cascade-select" @change="onCascadeSection">
                  <option :value="null" disabled>Select section...</option>
                  <option v-for="s in availableSourceSectionsForTarget" :key="'rs'+s.id" :value="s.id">{{ s.title }}</option>
                </select>
                <select v-if="newRule.sourceSectionId" v-model="newRule.sourceQuestionId" class="input rule-cascade-select" @change="onSourceQuestionChange">
                  <option :value="null" disabled>Select question...</option>
                  <option v-for="q in availableSourceQuestionsForTarget" :key="'rq'+q.id" :value="q.id">{{ q.title }} ({{ q.type.replace('_',' ') }})</option>
                </select>
              </div>
              <div v-if="availableSourcePagesForTarget.length > 0" class="rule-step">
                <label class="rule-step-label">2. Condition</label>
                <select v-model="newRule.op" class="input">
                  <option v-for="op in availableOperators" :key="op.value" :value="op.value">{{ op.label }}</option>
                </select>
                <select v-if="sourceQuestionHasOptions" v-model="newRule.value" class="input" style="margin-top:6px">
                  <option :value="null" disabled>Select value...</option>
                  <option v-for="opt in sourceQuestionOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <input v-else-if="newRule.op !== 'answered' && newRule.op !== 'not_answered' && newRule.op !== 'is_empty' && newRule.op !== 'not_empty'" v-model="newRule.value" class="input" style="margin-top:6px" :placeholder="sourceQuestionType === 'RATING' || sourceQuestionType === 'NUMBER' ? 'Enter number' : 'Value'" />
              </div>
              <div class="rule-step">
                <label class="rule-step-label">3. Rule Type</label>
                <div class="rule-logic-row">
                  <button type="button" class="rule-logic-btn" :class="{ 'rule-logic-btn--active': newRule.ruleType === 'AND' }" @click="newRule.ruleType = 'AND'">AND — all must match</button>
                  <button type="button" class="rule-logic-btn" :class="{ 'rule-logic-btn--active': newRule.ruleType === 'OR' }" @click="newRule.ruleType = 'OR'">OR — any can match</button>
                </div>
              </div>
              <div class="rule-form-btns">
                <button class="btn-secondary" style="flex:1;font-size:var(--text-xs)" @click="closeRuleForm">Cancel</button>
                <button class="btn-primary" style="flex:2;font-size:var(--text-xs)" :disabled="!newRule.sourceQuestionId || !newRule.op" @click="addRule">{{ editingRuleId ? 'Update Rule' : 'Add Rule' }}</button>
              </div>
            </div>
          </div>
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

          <!-- TABLE configuration -->
          <div v-if="editForm.type === 'TABLE'" class="table-config">
            <label class="form-label">Table Columns</label>
            <div v-for="(col, ci) in tableColumns" :key="ci" class="table-col-item">
              <div class="table-col-row">
                <input v-model="col.label" class="input" style="flex:1" placeholder="Column name" @blur="saveTableConfig" />
                <select v-model="col.type" class="input" style="width:100px" @change="onTableColTypeChange(ci)">
                  <option value="TEXT">Text</option>
                  <option value="DROPDOWN">Dropdown</option>
                  <option value="DATE">Date</option>
                  <option value="NUMBER">Number</option>
                </select>
                <button class="table-col-del" @click="removeTableColumn(ci)" :disabled="tableColumns.length <= 1" aria-label="Remove column">×</button>
              </div>
              <div v-if="col.type === 'DROPDOWN'" class="table-col-opts">
                <input v-for="(opt, oi) in (col._opts || [])" :key="oi" :value="opt"
                       class="input" style="font-size:11px;padding:4px 8px;margin-top:3px"
                       :placeholder="'Option ' + (oi+1)"
                       @blur="updateTableColOption(ci, oi, $event.target.value)"
                       @keyup.enter="updateTableColOption(ci, oi, $event.target.value); if (oi === (col._opts||[]).length - 1) addTableColOption(ci)" />
                <button class="table-col-add" @click="addTableColOption(ci)">+ Add option</button>
              </div>
            </div>
            <button class="table-col-add" @click="addTableColumn">+ Add Column</button>
            <div class="form-group" style="margin-top:8px">
              <label class="form-label">Initial Rows</label>
              <input v-model.number="tableRows" type="number" class="input" min="1" max="20" @change="saveTableConfig" style="width:80px" />
            </div>
          </div>

          <!-- Visibility Rules -->
          <div class="props-section">
            <h4 class="props-subtitle">Visibility Rules</h4>
            <div v-if="!currentTargetRules.length" class="props-empty">No rules — question is always visible</div>
            <template v-for="(grp, gi) in groupedCurrentTargetRules" :key="'g'+grp.group">
              <div class="rule-group-label">{{ gi === 0 ? 'AND — all must match' : 'OR — any can match' }}</div>
              <div v-for="rule in grp.rules" :key="'r'+rule.id" class="rule-row" :class="{ 'rule-row--broken': isRuleBroken(rule) }" @click="editRule(rule)">
                <span class="rule-text">
                  When {{ getQuestionTitle(rule.sourceQuestionId) }} {{ rule.op }} {{ getOptionLabel(rule.sourceQuestionId, rule.value) || rule.value || '' }}
                  <span v-if="isRuleBroken(rule)" class="rule-broken-badge" title="Source question options have changed — rule may not match">⚠</span>
                </span>
                <button class="rule-del" @click.stop="deleteRule(rule.id)" aria-label="Delete rule">×</button>
              </div>
            </template>
            <button v-if="!showRuleForm && canAddRuleForTarget" class="btn-secondary" style="width:100%;font-size:var(--text-xs);margin-top:8px" @click="openRuleForm">+ Add Rule</button>
            <div v-if="showRuleForm" class="rule-form">
              <div class="rule-step">
                <label class="rule-step-label">1. Source question</label>
                <select v-if="availableSourcePagesForTarget.length > 0" v-model="newRule.sourcePageId" class="input rule-cascade-select" @change="onCascadePage">
                  <option :value="null" disabled>Select page...</option>
                  <option v-for="p in availableSourcePagesForTarget" :key="'rp'+p.id" :value="p.id">{{ p.title }}</option>
                </select>
                <div v-else class="rule-empty-hint">No earlier questions exist to use as conditions</div>
                <select v-if="newRule.sourcePageId" v-model="newRule.sourceSectionId" class="input rule-cascade-select" @change="onCascadeSection">
                  <option :value="null" disabled>Select section...</option>
                  <option v-for="s in availableSourceSectionsForTarget" :key="'rs'+s.id" :value="s.id">{{ s.title }}</option>
                </select>
                <select v-if="newRule.sourceSectionId" v-model="newRule.sourceQuestionId" class="input rule-cascade-select" @change="onSourceQuestionChange">
                  <option :value="null" disabled>Select question...</option>
                  <option v-for="q in availableSourceQuestionsForTarget" :key="'rq'+q.id" :value="q.id">{{ q.title }} ({{ q.type.replace('_',' ') }})</option>
                </select>
              </div>
              <div v-if="availableSourcePagesForTarget.length > 0" class="rule-step">
                <label class="rule-step-label">2. Condition</label>
                <select v-model="newRule.op" class="input">
                  <option v-for="op in availableOperators" :key="op.value" :value="op.value">{{ op.label }}</option>
                </select>
                <select v-if="sourceQuestionHasOptions" v-model="newRule.value" class="input" style="margin-top:6px">
                  <option :value="null" disabled>Select value...</option>
                  <option v-for="opt in sourceQuestionOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <input v-else-if="newRule.op !== 'answered' && newRule.op !== 'not_answered' && newRule.op !== 'is_empty' && newRule.op !== 'not_empty'" v-model="newRule.value" class="input" style="margin-top:6px" :placeholder="sourceQuestionType === 'RATING' || sourceQuestionType === 'NUMBER' ? 'Enter number' : 'Value'" />
              </div>
              <div class="rule-step">
                <label class="rule-step-label">3. Rule Type</label>
                <div class="rule-logic-row">
                  <button type="button" class="rule-logic-btn" :class="{ 'rule-logic-btn--active': newRule.ruleType === 'AND' }" @click="newRule.ruleType = 'AND'">AND — all must match</button>
                  <button type="button" class="rule-logic-btn" :class="{ 'rule-logic-btn--active': newRule.ruleType === 'OR' }" @click="newRule.ruleType = 'OR'">OR — any can match</button>
                </div>
              </div>
              <div class="rule-form-btns">
                <button class="btn-secondary" style="flex:1;font-size:var(--text-xs)" @click="closeRuleForm">Cancel</button>
                <button class="btn-primary" style="flex:2;font-size:var(--text-xs)" :disabled="!newRule.sourceQuestionId || !newRule.op" @click="addRule">{{ editingRuleId ? 'Update Rule' : 'Add Rule' }}</button>
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
import request from '@/api/request'
import { updateTemplateApi, addPageApi, deletePageApi, updatePageApi, addSectionApi, deleteSectionApi, updateSectionApi, addQuestionApi, updateQuestionApi, deleteQuestionApi, addRuleApi, deleteRuleApi } from '@/api/survey'

const store = useSurveyStore()
const route = useRoute()
const router = useRouter()
const canvasCenter = ref(null)

function scrollCanvas(to) {
  const el = document.querySelector('.builder-center')
  if (!el) return
  el.scrollTo({ top: to === 'top' ? 0 : el.scrollHeight, behavior: 'smooth' })
}

const template = computed(() => store.currentTemplate)

const currentPageIndex = ref(0)
const currentSectionIndex = ref(0)
const currentPage = computed(() => template.value?.pages?.[currentPageIndex.value] || null)
const currentSection = computed(() => currentPage.value?.sections?.[currentSectionIndex.value] || null)
const selectedQuestion = ref(null)
const showRuleForm = ref(false)
const editingRuleId = ref(null) // non-null when editing an existing rule
const editingTab = ref(null) // 'p'+pageId or 's'+sectionId when editing tab title
const showAddQ = ref(false)
const showAddQSection = ref(null)

// Rule target — can be QUESTION, SECTION, or PAGE
const ruleTarget = reactive({ type: 'QUESTION', id: null })
function openRuleFor(type, id) {
  ruleTarget.type = type
  ruleTarget.id = id
  if (type !== 'QUESTION') selectedQuestion.value = null
  showRuleForm.value = false
}
const canAddRuleForTarget = computed(() => {
  if (!template.value?.pages) return false
  // Determine the effective "earlier questions" set based on target type
  const earlierQuestions = effectiveQuestionsBefore.value
  return earlierQuestions.length > 0
})

function countRulesFor(type, id) {
  if (!template.value?.pages) return 0
  let count = 0
  for (const p of template.value.pages) {
    if (type === 'PAGE' && p.id === id) count += (p.visibilityRules || []).length
    for (const s of p.sections || []) {
      if (type === 'SECTION' && s.id === id) count += (s.visibilityRules || []).length
    }
  }
  return count
}
// Get rules for the current rule target (page/section/question)
const currentTargetRules = computed(() => {
  if (!template.value?.pages) return []
  if (ruleTarget.type === 'PAGE') {
    for (const p of template.value.pages) {
      if (p.id === ruleTarget.id) return p.visibilityRules || []
    }
  }
  if (ruleTarget.type === 'SECTION') {
    for (const p of template.value.pages) {
      for (const s of p.sections || []) {
        if (s.id === ruleTarget.id) return s.visibilityRules || []
      }
    }
  }
  if (ruleTarget.type === 'QUESTION') {
    const q = allQuestions.value.find(q => q.id === ruleTarget.id)
    return q?.visibilityRules || []
  }
  return []
})
// Group rules by ruleType for AND/OR display
const groupedCurrentTargetRules = computed(() => {
  const rules = currentTargetRules.value || []
  const groups = []
  // AND rules first, then OR rules
  const andRules = rules.filter(r => (r.ruleType || 'AND') === 'AND')
  const orRules = rules.filter(r => r.ruleType === 'OR')
  if (andRules.length > 0) groups.push({ group: 'AND', rules: andRules })
  if (orRules.length > 0) groups.push({ group: 'OR', rules: orRules })
  return groups
})

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
const tableColumns = reactive([{ label: 'Item', type: 'TEXT', key: 'col_1' }])
const tableRows = ref(3)
const newRule = reactive({ sourcePageId: null, sourceSectionId: null, sourceQuestionId: null, op: 'eq', value: '', ruleType: 'AND' })

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

// When a page or section is selected as rule target but no question is selected
const isShowingNonQTarget = computed(() =>
  (ruleTarget.type === 'PAGE' || ruleTarget.type === 'SECTION') && ruleTarget.id && !selectedQuestion.value
)

// All questions up to (and including) all questions in pages before the rule target page
const questionsBeforeTarget = computed(() => {
  if (!template.value?.pages) return []
  if (ruleTarget.type === 'PAGE') {
    // For PAGE rules: source must be from pages before this page
    const result = []
    for (const p of template.value.pages) {
      if (p.id === ruleTarget.id) break
      for (const s of p.sections || []) {
        for (const q of s.questions || []) result.push(q)
      }
    }
    return result
  }
  if (ruleTarget.type === 'SECTION') {
    // For SECTION rules: source must be from sections before this section (same page or earlier pages)
    const result = []
    let found = false
    for (const p of template.value.pages) {
      for (const s of p.sections || []) {
        if (s.id === ruleTarget.id) { found = true; break }
        for (const q of s.questions || []) result.push(q)
      }
      if (found) break
    }
    return result
  }
  return allQuestions.value
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
// Source selector computeds for non-question targets (PAGE/SECTION)
const effectiveQuestionsBefore = computed(() => {
  if (selectedQuestion.value) return questionsBeforeCurrent.value
  return questionsBeforeTarget.value
})
const availableSourcePagesForTarget = computed(() => {
  if (!template.value?.pages) return []
  const pages = []
  for (const p of template.value.pages) {
    const hasBefore = p.sections?.some(s => s.questions?.some(q =>
      effectiveQuestionsBefore.value.some(eq => eq.id === q.id)
    ))
    if (hasBefore) pages.push(p)
  }
  return pages
})
const availableSourceSectionsForTarget = computed(() => {
  if (!newRule.sourcePageId || !template.value?.pages) return []
  const page = template.value.pages.find(p => p.id === newRule.sourcePageId)
  if (!page) return []
  return (page.sections || []).filter(s => s.questions?.some(q =>
    effectiveQuestionsBefore.value.some(eq => eq.id === q.id)
  ))
})
const availableSourceQuestionsForTarget = computed(() => {
  if (!newRule.sourceSectionId || !template.value?.pages) return []
  for (const p of template.value.pages) {
    for (const s of p.sections || []) {
      if (s.id === newRule.sourceSectionId) {
        return (s.questions || []).filter(q =>
          effectiveQuestionsBefore.value.some(eq => eq.id === q.id)
        )
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
  editingRuleId.value = null
  newRule.sourcePageId = null; newRule.sourceSectionId = null; newRule.sourceQuestionId = null; newRule.op = 'eq'; newRule.value = ''
  newRule.ruleType = 'AND'
  showRuleForm.value = true
}
function editRule(rule) {
  // If already editing this rule, close the form (toggle behavior)
  if (editingRuleId.value === rule.id && showRuleForm.value) {
    closeRuleForm()
    return
  }
  editingRuleId.value = rule.id
  // Populate form with existing rule values
  // Find source question's page and section for cascade selects
  newRule.sourceQuestionId = rule.sourceQuestionId
  newRule.op = rule.op
  newRule.value = rule.value || ''
  newRule.ruleType = rule.ruleType || 'AND'
  // Resolve page and section from the source question
  newRule.sourcePageId = null; newRule.sourceSectionId = null
  for (const p of template.value.pages || []) {
    for (const s of p.sections || []) {
      for (const q of s.questions || []) {
        if (q.id === rule.sourceQuestionId) {
          newRule.sourceSectionId = s.id
          newRule.sourcePageId = p.id
        }
      }
    }
  }
  showRuleForm.value = true
}
function closeRuleForm() {
  editingRuleId.value = null
  newRule.sourcePageId = null; newRule.sourceSectionId = null; newRule.sourceQuestionId = null; newRule.op = 'eq'; newRule.value = ''; newRule.ruleType = 'AND'
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

function switchPage(idx) {
  currentPageIndex.value = idx
  currentSectionIndex.value = 0
  selectedQuestion.value = null
  // Reset rule target so previous page's rules don't persist in the right panel
  ruleTarget.type = 'QUESTION'
  ruleTarget.id = null
  showRuleForm.value = false
}
function toggleSection(key) { expandedNodes[key] = !expandedNodes[key] }

function selectQuestion(q) {
  if (selectedQuestion.value?.id === q.id) return // already selected
  selectedQuestion.value = q
  ruleTarget.type = 'QUESTION'
  ruleTarget.id = q.id
  editForm.title = q.title
  editForm.type = q.type
  editForm.required = q.required
  // Load TABLE config if applicable
  if (q.type === 'TABLE') loadTableConfig(q)
  else { tableColumns.length = 0; tableColumns.push({ label: 'Item', type: 'TEXT', key: 'col_1' }); tableRows.value = 3 }
}
function loadTableConfig(q) {
  try {
    const cfg = JSON.parse(q.options || '{}')
    const cols = cfg.columns || [{ label: 'Item', type: 'TEXT', key: 'col_1' }]
    tableColumns.length = 0
    cols.forEach(c => {
      tableColumns.push({
        key: c.key, label: c.label, type: c.type,
        _opts: c.type === 'DROPDOWN' ? (c.options && c.options.length ? [...c.options] : ['']) : undefined
      })
    })
    tableRows.value = cfg.rows || 3
  } catch {
    tableColumns.length = 0; tableColumns.push({ label: 'Item', type: 'TEXT', key: 'col_1' })
    tableRows.value = 3
  }
}
function addTableColumn() {
  tableColumns.push({ label: 'Col ' + (tableColumns.length + 1), type: 'TEXT', key: 'col_' + Date.now() })
  saveTableConfig()
}
function removeTableColumn(idx) {
  if (tableColumns.length <= 1) return
  tableColumns.splice(idx, 1)
  saveTableConfig()
}
function onTableColTypeChange(ci) {
  const col = tableColumns[ci]
  if (col.type === 'DROPDOWN') {
    if (!col._opts || col._opts.length === 0) col._opts = ['']
  } else {
    delete col._opts
  }
  saveTableConfig()
}
function addTableColOption(ci) {
  const col = tableColumns[ci]
  if (!col._opts) col._opts = []
  col._opts.push('')
}
function updateTableColOption(ci, oi, value) {
  const col = tableColumns[ci]
  if (!col._opts) return
  col._opts[oi] = value
  saveTableConfig()
}
function saveTableConfig() {
  if (selectedQuestion.value?.type !== 'TABLE') return
  const cols = tableColumns.map(c => {
    const col = { key: c.key, label: c.label, type: c.type }
    if (c.type === 'DROPDOWN' && c._opts) col.options = c._opts.filter(Boolean)
    return col
  })
  const options = JSON.stringify({ columns: cols, rows: tableRows.value })
  selectedQuestion.value.options = options
  updateQuestionApi(selectedQuestion.value.id, { options }).catch(e => {
    ElMessage.error('Failed to save table config')
    console.error('[Builder] saveTableConfig:', e)
  })
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
  updateQuestionApi(q.id, { options: newOptionsJson }).catch(e => {
    ElMessage.error('Failed to save options')
    console.error('[Builder] saveOption:', e)
  })
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
  try {
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
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to save changes')
    console.error('[Builder] saveAndReturn:', e)
  }
}
async function publishFromBuilder() {
  await savePendingEdits()
  const errors = []
  for (const page of template.value.pages || []) {
    for (const section of page.sections || []) {
      if (!section.questions || section.questions.length === 0) {
        errors.push(`Section "${section.title}" in "${page.title}" has no questions`)
        continue
      }
      for (const q of section.questions) {
        if (q.type === 'SINGLE_CHOICE' || q.type === 'MULTI_CHOICE' || q.type === 'DROPDOWN') {
          const opts = JSON.parse(q.options || '{}').options || []
          if (opts.length === 0) errors.push(`"${q.title}" has no options configured`)
        }
        if (q.type === 'RATING') {
          const max = JSON.parse(q.options || '{}').max
          if (!max || max < 2) errors.push(`"${q.title}" rating max must be at least 2`)
        }
        if (q.type === 'TABLE') {
          const cols = JSON.parse(q.options || '{}').columns || []
          for (const col of cols) {
            if (col.type === 'DROPDOWN' && (!col.options || col.options.length === 0)) {
              errors.push(`Table "${q.title}" column "${col.label}" is DROPDOWN but has no options`)
            }
          }
        }
      }
    }
  }
  if (errors.length > 0) {
    await ElMessageBox.alert(
      errors.map((e, i) => `${i + 1}. ${e}`).join('<br/>'),
      `Cannot Publish — ${errors.length} issue${errors.length > 1 ? 's' : ''}`,
      { confirmButtonText: 'OK', type: 'warning', dangerouslyUseHTMLString: true }
    )
    return
  }
  try {
    await ElMessageBox.confirm('Publish this template? Once published, instances can be distributed.', 'Publish', { type: 'info' })
  } catch { return }
  const { data } = await updateTemplateApi(template.value.id, { status: 'PUBLISHED' })
  if (data.code === 200) {
    ElMessage.success('Published!')
    router.push(`/admin/surveys/${template.value.id}/results`)
  } else ElMessage.error(data.message)
}

async function savePendingEdits() {
  try {
    if (currentPage.value) {
      await updatePageApi(currentPage.value.id, { title: currentPage.value.title })
    }
    for (const s of currentPage.value?.sections || []) {
      if (sectionTitles[s.id] && sectionTitles[s.id] !== s.title) {
        await updateSectionApi(s.id, { title: sectionTitles[s.id] })
      }
      for (const q of s.questions || []) {
        if (questionTitles[q.id] && questionTitles[q.id] !== q.title) {
          await updateQuestionApi(q.id, { title: questionTitles[q.id] })
        }
        if (hasOptions(q.type) && optionsCache[q.id]) {
          const items = optionsCache[q.id].filter(o => o.label.trim())
          if (items.length > 0) {
            await updateQuestionApi(q.id, { options: JSON.stringify({ options: items.map(o => ({ key: o.key, label: o.label.trim() })) }) })
          }
        }
      }
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to save edits')
    console.error('[Builder] savePendingEdits:', e)
  }
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
function parseTableColumns(optionsJson) {
  try { return JSON.parse(optionsJson || '{}').columns || [{ key: 'col_1', label: 'Item', type: 'TEXT' }] }
  catch { return [{ key: 'col_1', label: 'Item', type: 'TEXT' }] }
}
function parseTableRows(optionsJson) {
  try { return JSON.parse(optionsJson || '{}').rows || 3 }
  catch { return 3 }
}
function hasOptions(type) { return ['SINGLE_CHOICE','MULTI_CHOICE','DROPDOWN','CASCADER'].includes(type) }
function getQuestionErrors(q) {
  const errs = []
  if (q.type === 'SINGLE_CHOICE' || q.type === 'MULTI_CHOICE' || q.type === 'DROPDOWN') {
    const opts = JSON.parse(q.options || '{}').options || []
    if (opts.length === 0) errs.push('No options')
  }
  if (q.type === 'RATING') {
    const max = JSON.parse(q.options || '{}').max
    if (!max || max < 2) errs.push('Need ≥2 stars')
  }
  if (q.type === 'TABLE') {
    const cols = JSON.parse(q.options || '{}').columns || []
    for (const col of cols) {
      if (col.type === 'DROPDOWN' && (!col.options || col.options.length === 0)) {
        errs.push('Col missing options'); break
      }
    }
  }
  return errs
}
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

async function updatePageTitle(page, title) {
  if (!title || title === page.title) return
  page.title = title
  await updatePageApi(page.id, { title })
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
  try {
    await deletePageApi(pageId)
    ElMessage.success('Page deleted')
    currentPageIndex.value = Math.min(currentPageIndex.value, (template.value.pages?.length || 1) - 1)
    await store.fetchTemplate(template.value.id)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to delete page')
    console.error('[Builder] deletePage:', e)
  }
}

// ── Section Actions ──

async function saveSectionTitle(sectionId) {
  const newTitle = sectionTitles[sectionId]
  if (!newTitle) return
  await updateSectionApi(sectionId, { title: newTitle })
}
async function addSection(pageId) {
  try {
    const { data } = await addSectionApi(pageId, { title: 'New Section' })
    if (data.code === 200) { ElMessage.success('Section added'); await store.fetchTemplate(template.value.id) }
    else ElMessage.error(data.message)
  } catch (e) {
    ElMessage.error('Failed to add section')
    console.error('[Builder] addSection:', e)
  }
}
async function deleteSection(sectionId) {
  try { await ElMessageBox.confirm('Delete this section?', 'Delete', { type: 'warning' }) } catch { return }
  try {
    await deleteSectionApi(sectionId)
    ElMessage.success('Section deleted')
    await store.fetchTemplate(template.value.id)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to delete section')
    console.error('[Builder] deleteSection:', e)
  }
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
  let title = 'New Question'
  let options = undefined
  if (type === 'SINGLE_CHOICE') {
    title = 'Choose one'
    options = JSON.stringify({ options: [{ key: 'opt_1', label: 'Option 1' }, { key: 'opt_2', label: 'Option 2' }, { key: 'opt_3', label: 'Option 3' }] })
  } else if (type === 'MULTI_CHOICE') {
    title = 'Select all that apply'
    options = JSON.stringify({ options: [{ key: 'opt_1', label: 'Option 1' }, { key: 'opt_2', label: 'Option 2' }, { key: 'opt_3', label: 'Option 3' }] })
  } else if (type === 'DROPDOWN') {
    title = 'Choose from list'
    options = JSON.stringify({ options: [{ key: 'opt_1', label: 'Option 1' }, { key: 'opt_2', label: 'Option 2' }, { key: 'opt_3', label: 'Option 3' }] })
  } else if (type === 'RATING') {
    title = 'Rate from 1-5'
    options = JSON.stringify({ max: 5, value: 3 })
  }
  try {
    const { data } = await addQuestionApi(sectionId, { type, title, required: 0, options })
    if (data.code === 200) { ElMessage.success('Question added'); await store.fetchTemplate(template.value.id) }
    else ElMessage.error(data.message)
  } catch (e) {
    ElMessage.error('Failed to add question')
    console.error('[Builder] addQuestion:', e)
  }
}
async function saveQuestionProperties() {
  if (!selectedQuestion.value) return
  try {
    let options = selectedQuestion.value.options
    if (hasOptions(editForm.type)) {
      const items = (optionsCache[selectedQuestion.value.id] || []).filter(o => o.label.trim())
      const newOpts = items.map(o => ({ key: o.key, label: o.label.trim() }))
      options = JSON.stringify({ options: newOpts })
    }
    await updateQuestionApi(selectedQuestion.value.id, {
      title: editForm.title, type: editForm.type, required: editForm.required ? 1 : 0, options
    })
    ElMessage.success('Properties saved')
    await store.fetchTemplate(template.value.id)
    const updated = allQuestions.value.find(q => q.id === selectedQuestion.value.id)
    if (updated) selectQuestion(updated)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to save properties')
    console.error('[Builder] saveQuestionProperties:', e)
  }
}
async function moveQuestion(section, fromIdx, direction) {
  const questions = section.questions
  const toIdx = fromIdx + direction
  if (toIdx < 0 || toIdx >= questions.length) return
  // Swap in local array for instant UI
  const tmp = questions[fromIdx]
  questions[fromIdx] = questions[toIdx]
  questions[toIdx] = tmp
  // Build reorder items and persist
  const items = questions.map((q, i) => ({ id: q.id, displayOrder: i + 1 }))
  const { data } = await request.put(`/admin/surveys/sections/${section.id}/questions/reorder`, { items })
  if (data?.code !== 200) {
    // Revert on failure
    const tmp2 = questions[fromIdx]
    questions[fromIdx] = questions[toIdx]
    questions[toIdx] = tmp2
  }
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
  if (!newRule.sourceQuestionId) return
  const targetId = ruleTarget.type === 'QUESTION' ? (selectedQuestion.value?.id) : ruleTarget.id
  if (!targetId) return
  // Contradiction check: same ruleType (AND), same sourceQuestion, contradictory condition
  // Skip the rule being edited when checking contradictions
  const existing = (currentTargetRules.value || []).filter(r => r.id !== editingRuleId.value)
  for (const r of existing) {
    if (r.sourceQuestionId === newRule.sourceQuestionId && r.ruleType === newRule.ruleType) {
      const contradicts =
        (r.op === 'eq' && newRule.op === 'neq' && r.value === newRule.value) ||
        (r.op === 'neq' && newRule.op === 'eq' && r.value === newRule.value) ||
        (r.op === 'eq' && newRule.op === 'eq' && r.value !== newRule.value) ||
        (r.op === 'in' && newRule.op === 'not_in' && r.value === newRule.value) ||
        (r.op === 'not_in' && newRule.op === 'in' && r.value === newRule.value) ||
        (r.op === 'answered' && newRule.op === 'not_answered') ||
        (r.op === 'not_answered' && newRule.op === 'answered')
      if (contradicts) {
        ElMessage.warning('This rule contradicts an existing AND rule — would always be false')
        return
      }
    }
  }
  // If editing, delete the old rule first
  if (editingRuleId.value) {
    await deleteRuleApi(editingRuleId.value)
  }
  const { data } = await addRuleApi(template.value.id, {
    targetType: ruleTarget.type, targetId: targetId,
    sourceQuestionId: newRule.sourceQuestionId, op: newRule.op,
    value: newRule.value || '', ruleType: newRule.ruleType
  })
  if (data.code === 200) {
    editingRuleId.value = null
    newRule.sourceQuestionId = null; newRule.op = 'eq'; newRule.value = ''
    showRuleForm.value = false
    await store.fetchTemplate(template.value.id)
    if (ruleTarget.type === 'QUESTION') {
      const updated = allQuestions.value.find(q => q.id === selectedQuestion.value?.id)
      if (updated) selectQuestion(updated)
    }
  }
}
async function deleteRule(ruleId) {
  await deleteRuleApi(ruleId)
  showRuleForm.value = false
  await store.fetchTemplate(template.value.id)
  if (ruleTarget.type === 'QUESTION') {
    const updated = allQuestions.value.find(q => q.id === selectedQuestion.value?.id)
    if (updated) selectQuestion(updated)
  }
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
.canvas-scroll-btns { position: sticky; bottom: 16px; display: flex; gap: 4px; justify-content: flex-end; padding: 0 var(--space-sm); pointer-events: none; }
.scroll-btn { pointer-events: auto; padding: 6px 12px; font-size: 10px; font-weight: 500; font-family: var(--font-body); color: var(--color-text-muted); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm); cursor: pointer; box-shadow: var(--shadow-sm); transition: all var(--transition-fast); }
.scroll-btn:hover { color: var(--color-primary); border-color: var(--color-primary); }

.builder-layout { display: flex; flex: 1; overflow: hidden; }

/* Top page tab bar */
.canvas-page-tabs { display: flex; align-items: center; gap: 4px; margin-bottom: var(--space-sm); padding-bottom: var(--space-sm); border-bottom: 2px solid var(--color-gray-200); overflow-x: auto; max-width: 900px; margin-left: auto; margin-right: auto; }
.page-tab { display: flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: var(--radius-md) var(--radius-md) 0 0; cursor: pointer; font-size: var(--text-sm); font-family: var(--font-body); font-weight: 500; background: none; border: none; color: var(--color-text-secondary); transition: all var(--transition-fast); white-space: nowrap; min-height: 40px; }
.page-tab:hover { background: var(--color-gray-100); color: var(--color-text-primary); }
.page-tab--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 700; box-shadow: inset 0 -2px 0 var(--color-primary); }
.page-tab-num { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; background: var(--color-gray-200); border-radius: 50%; flex-shrink: 0; }
.page-tab--active .page-tab-num { background: var(--color-primary); color: white; }
.page-tab-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.page-tab-del { width: 22px; height: 22px; font-size: 14px; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; display: none; }
.page-tab:hover .page-tab-del { display: inline-flex; align-items: center; justify-content: center; }
.page-tab-del:hover { background: #FEE2E2; color: #B91C1C; }
.page-tab--add { font-weight: 600; color: var(--color-primary); border: 1px dashed var(--color-gray-300); border-radius: var(--radius-md); padding: 8px 14px; margin-left: 4px; box-shadow: none; }
.page-tab--add:hover { border-color: var(--color-primary); background: var(--color-primary-bg); }

/* Center: Canvas */
.builder-center { flex: 1; overflow-y: auto; padding: var(--space-lg) var(--space-xl); background: #F8F9FB; position: relative; }

/* Section tabs — horizontal like page tabs */
.canvas-section-tabs { display: flex; align-items: center; gap: 4px; margin-bottom: var(--space-sm); padding-bottom: var(--space-sm); border-bottom: 1px solid var(--color-gray-200); overflow-x: auto; max-width: 900px; margin-left: auto; margin-right: auto; }
.section-tab { display: flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: var(--radius-md) var(--radius-md) 0 0; cursor: pointer; font-size: var(--text-xs); font-family: var(--font-body); font-weight: 500; background: none; border: none; color: var(--color-text-secondary); transition: all var(--transition-fast); white-space: nowrap; min-height: 36px; }
.section-tab:hover { background: var(--color-gray-50); color: var(--color-text-primary); }
.section-tab--active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 700; box-shadow: inset 0 -2px 0 var(--color-primary); }
.section-tab-num { width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: 700; background: var(--color-gray-200); border-radius: 50%; flex-shrink: 0; }
.section-tab--active .section-tab-num { background: var(--color-primary); color: white; }
.section-tab-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.section-tab-qs { font-size: 9px; color: var(--color-text-muted); }
.section-tab--active .section-tab-qs { color: var(--color-primary); opacity: 0.7; }
.section-tab-del { width: 20px; height: 20px; font-size: 12px; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; display: none; }
.section-tab:hover .section-tab-del { display: inline-flex; align-items: center; justify-content: center; }
.section-tab-del:hover { background: #FEE2E2; color: #B91C1C; }
.section-tab--add { font-weight: 600; color: var(--color-primary); border: 1px dashed var(--color-gray-200); border-radius: var(--radius-md); padding: 6px 12px; margin-left: 4px; box-shadow: none; }
.section-tab--add:hover { border-color: var(--color-primary); background: var(--color-primary-bg); }
.canvas { max-width: 900px; margin: 0 auto; }

/* Page tab title */
.page-tab-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
/* Page tab inline title editing (double-click) */
.page-tab-title-input { width: 80px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); border: 1px solid var(--color-primary); background: var(--color-white); padding: 2px 4px; border-radius: var(--radius-sm); color: var(--color-text-primary); outline: none; }
.page-tab-title-input:focus { width: 140px; }
.page-tab--active .page-tab-title-input { font-weight: 700; }

/* Section tab title */
.section-tab-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
/* Section tab inline title editing (double-click) */
.section-tab-title-input { width: 70px; font-size: var(--text-xs); font-weight: 500; font-family: var(--font-body); border: 1px solid var(--color-primary); background: var(--color-white); padding: 1px 3px; border-radius: var(--radius-sm); color: var(--color-text-primary); outline: none; }
.section-tab-title-input:focus { width: 120px; }
.section-tab--active .section-tab-title-input { font-weight: 700; }

/* Actions in tab bar */
.canvas-tab-actions { margin-left: auto; display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.canvas-section-actions { display: flex; align-items: center; gap: 8px; margin-bottom: var(--space-md); }
.canvas-add-section { padding: 10px 20px; font-size: var(--text-sm); font-weight: 500; color: var(--color-primary); background: var(--color-primary-bg); border: 1px dashed var(--color-primary); border-radius: var(--radius-md); cursor: pointer; white-space: nowrap; min-height: 44px; }
.canvas-add-section:hover { background: var(--color-primary); color: white; }

.canvas-section-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0 0 var(--space-sm); }
.inline-rules-btn { padding: 10px 20px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-primary); background: var(--color-primary-bg); border: 1px dashed var(--color-primary); border-radius: var(--radius-md); cursor: pointer; white-space: nowrap; min-height: 44px; transition: all var(--transition-fast); }
.inline-rules-btn:hover { background: var(--color-primary); color: var(--color-white); }
.inline-rules-btn--empty { color: var(--color-text-muted); background: var(--color-gray-50); border-color: var(--color-gray-200); cursor: default; }
.inline-rules-btn--empty:hover { background: var(--color-gray-50); color: var(--color-text-muted); }
.canvas-section-del { width: 32px; height: 32px; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; visibility: hidden; display: flex; align-items: center; justify-content: center; }
.canvas-section:hover .canvas-section-del { visibility: visible; }
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
.canvas-question--error { border-left-color: #DC2626; }
.canvas-question--error .canvas-q-title-input { color: #DC2626; }
.canvas-q-errors { margin-top: 6px; font-size: 10px; color: #DC2626; font-weight: 500; }
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
.canvas-q-arrow { width: 28px; height: 28px; padding: 0; background: var(--color-white); border: 1px solid var(--color-gray-200); color: var(--color-text-secondary); cursor: pointer; border-radius: var(--radius-sm); visibility: hidden; transition: all var(--transition-fast); display: inline-flex; align-items: center; justify-content: center; }
.canvas-q-arrow svg { width: 14px; height: 14px; }
.canvas-q-arrow:hover { background: var(--color-primary-bg); color: var(--color-primary); border-color: var(--color-primary); }
.canvas-question:hover .canvas-q-arrow { visibility: visible; }
.canvas-q-del { width: 32px; height: 32px; min-width: 32px; padding: 0; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; visibility: hidden; display: flex; align-items: center; justify-content: center; }
.canvas-q-del svg { width: 14px; height: 14px; }
.canvas-question:hover .canvas-q-del { visibility: visible; }
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

.rule-row { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm); margin-bottom: 4px; font-size: var(--text-xs); min-height: 36px; cursor: pointer; transition: border-color var(--transition-fast), background var(--transition-fast); }
.rule-row:hover { border-color: var(--color-primary-light); background: var(--color-primary-bg); }
.rule-row--broken { border-color: #FCD34D; background: #FFFBEB; }
.rule-text { flex: 1; color: var(--color-text-secondary); display: flex; align-items: center; gap: 4px; }
.rule-broken-badge { font-size: 11px; cursor: help; }
.rule-del { width: 24px; height: 24px; padding: 0; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; }
.rule-group-label { font-size: 10px; font-weight: 700; padding: 3px 0 4px; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.3px; }
.rule-form { margin-top: var(--space-sm); display: flex; flex-direction: column; gap: var(--space-md); }
.rule-step { background: var(--color-gray-50); border-radius: var(--radius-md); padding: var(--space-sm); }
.rule-step-label { display: block; font-size: 10px; font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
.rule-cascade-select { margin-top: 4px; }
.rule-logic-row { display: flex; gap: 4px; }
.rule-logic-btn { flex: 1; padding: 8px 12px; font-size: var(--text-xs); font-weight: 600; font-family: var(--font-body); color: var(--color-text-muted); background: var(--color-white); border: 2px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; transition: all var(--transition-fast); min-height: 36px; }
.rule-logic-btn:hover { border-color: var(--color-primary-light); color: var(--color-primary); }
.rule-logic-btn--active { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-bg); }
.rule-logic-hint { font-size: 10px; color: var(--color-text-muted); margin-top: 4px; font-style: italic; }
.rule-empty-hint { font-size: var(--text-xs); color: var(--color-text-muted); padding: 8px 0; font-style: italic; }
.rule-step .input:not(:first-child) { margin-top: 4px; }
.rule-form-btns { display: flex; gap: 6px; margin-top: 2px; }

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

/* Table config */
.table-config { border-top: 1px solid var(--color-gray-200); padding-top: var(--space-md); margin-bottom: var(--space-sm); display: flex; flex-direction: column; gap: 4px; }
.table-col-row { display: flex; align-items: center; gap: 6px; }
.table-col-del { width: 28px; height: 28px; padding: 0; background: none; border: none; color: var(--color-text-muted); cursor: pointer; border-radius: 3px; font-size: 16px; }
.table-col-del:hover:not(:disabled) { background: #FEE2E2; color: #B91C1C; }
.table-col-del:disabled { opacity: 0.3; cursor: not-allowed; }
.table-col-item { margin-bottom: 6px; }
.table-col-add { padding: 4px 8px; font-size: var(--text-xs); color: var(--color-primary); background: none; border: none; cursor: pointer; text-align: left; }
.table-col-add:hover { text-decoration: underline; }
.table-col-opts { margin-left: 6px; padding-left: 8px; border-left: 2px solid var(--color-gray-200); }

/* Canvas table preview */
.canvas-table { width: 100%; border-collapse: collapse; font-size: var(--text-xs); margin-top: 4px; }
.canvas-table th, .canvas-table td { border: 1px solid var(--color-gray-200); padding: 4px 8px; text-align: left; }
.canvas-table th { background: var(--color-gray-50); font-weight: 600; color: var(--color-text-secondary); }
.canvas-table td { color: var(--color-text-muted); }
.btn-publish { padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: #059669; border: none; border-radius: var(--radius-md); cursor: pointer; min-height: 44px; transition: opacity var(--transition-fast); }
.btn-publish:hover { opacity: 0.9; }
.btn-primary { padding: 10px 20px; font-weight: 600; font-family: var(--font-body); color: var(--color-white); background: var(--color-primary); border: none; border-radius: var(--radius-md); cursor: pointer; font-size: var(--text-sm); min-height: 44px; }
.btn-primary:hover:not(:disabled) { opacity: 0.9; }
.btn-secondary { padding: 10px 20px; font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; font-size: var(--text-sm); min-height: 44px; }
.btn-secondary:hover { background: var(--color-gray-50); }

@media (max-width: 1024px) {
  .builder-right { width: 260px; }
}
@media (max-width: 768px) {
  .builder-layout { flex-direction: column; }
  .builder-right { width: 100%; max-height: 200px; }
}
</style>
