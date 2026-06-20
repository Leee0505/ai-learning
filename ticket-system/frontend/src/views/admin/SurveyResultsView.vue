<template>
  <div class="results-page">
    <div class="results-topbar">
      <button class="btn-back" @click="$router.push('/admin/surveys')" aria-label="Back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15,18 9,12 15,6"/></svg>
        Back
      </button>
      <h1 class="results-title">{{ results?.templateTitle || 'Results' }}</h1>
      <span class="results-summary">{{ results?.completedInstances || 0 }}/{{ results?.totalInstances || 0 }} completed</span>
    </div>

    <div v-if="!results" class="loading">Loading...</div>

    <div v-else class="results-body">
      <div v-for="q in questionsWithLabels" :key="q.questionId" class="result-card">
        <h3 class="result-q-title">{{ q.title }} <span class="result-q-type">{{ q.type.replace('_',' ') }}</span></h3>

        <!-- Choice questions: bar chart -->
        <div v-if="q.choiceCounts" class="result-bars">
          <div v-for="(count, opt) in q.choiceCounts" :key="opt" class="result-bar-row">
            <span class="result-bar-label">{{ opt }}</span>
            <div class="result-bar-track">
              <div class="result-bar-fill" :style="{ width: barWidth(count, q.choiceCounts) + '%' }"></div>
              <span class="result-bar-count">{{ count }}</span>
            </div>
          </div>
        </div>

        <!-- Text questions: answer list -->
        <div v-if="q.textAnswers" class="result-texts">
          <div v-if="q.textAnswers.length === 0" class="muted">No answers yet</div>
          <div v-for="(ans, ai) in q.textAnswers" :key="ai" class="result-text-item">{{ ans }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/request'
import { getTemplateApi } from '@/api/survey'

const route = useRoute()
const results = ref(null)
const template = ref(null)

// Parse options to get key→label mapping
function parseOptions(optionsJson) {
  try {
    const o = JSON.parse(optionsJson || '{}')
    const raw = o.options || []
    if (raw.length === 0) return []
    return typeof raw[0] === 'object' ? raw : raw.map(s => ({ key: s, label: s }))
  } catch { return [] }
}

// Get label by key for a given question
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

// Merge choiceCounts with labels — maps keys to labels for display
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

function barWidth(count, counts) {
  const max = Math.max(...Object.values(counts), 1)
  return Math.round((count / max) * 100)
}

onMounted(async () => {
  const id = route.params.id
  const { data } = await request.get(`/admin/surveys/${id}/results`)
  if (data.code === 200) results.value = data.data
  // Fetch template for option key→label mapping
  try {
    const t = await getTemplateApi(id)
    if (t.data?.code === 200) template.value = t.data.data
  } catch { /* no template, fall back to raw keys */ }
})
</script>

<style scoped>
.results-page { max-width: 800px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.results-topbar { display: flex; align-items: center; gap: var(--space-md); margin-bottom: var(--space-xl); }
.btn-back { display: flex; align-items: center; padding: 6px 12px; background: none; border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.btn-back svg { width: 16px; height: 16px; }
.results-title { flex: 1; font-size: var(--text-xl); font-weight: 700; margin: 0; }
.results-summary { font-size: var(--text-sm); color: var(--color-text-muted); }
.loading { text-align: center; padding: var(--space-2xl); color: var(--color-text-muted); }

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

@media (max-width: 768px) {
  .result-bar-label { width: 60px; font-size: var(--text-xs); }
}
</style>
