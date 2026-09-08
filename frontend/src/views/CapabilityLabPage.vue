<template>
  <Layout @home="router.push('/')" @customer-service="router.push('/customer-service')" @lab="router.push('/lab')" @select-chat="id => router.push('/chat/' + id)" @new-chat="router.push('/')">
    <div class="h-full overflow-y-auto">
      <header class="border-b border-slate-200/70 bg-white px-6 py-5">
        <div class="flex items-center gap-2 text-sm font-semibold text-slate-800"><WandSparkles :size="17" class="text-[#4d6bfe]" />AI 工具中心</div>
        <p class="mt-1 text-xs text-slate-400">直接处理提示词、结构化信息、实时资料和知识库问题</p>
      </header>
      <main class="mx-auto grid max-w-6xl gap-4 px-5 py-6 lg:grid-cols-2">
        <section class="tool-panel">
          <div class="tool-heading"><TextCursorInput :size="17" class="text-indigo-500" /><div><h2>提示词助手</h2><p>生成适合初学者阅读的主题说明</p></div></div>
          <div class="mt-4 flex gap-2"><input v-model="topic" class="tool-input" placeholder="输入技术或主题，例如：Spring AI" @keydown.enter="runPrompt" /><button class="tool-button bg-[#4d6bfe]" :disabled="busy === 'prompt'" @click="runPrompt"><LoaderCircle v-if="busy === 'prompt'" class="animate-spin" :size="15" /><Play v-else :size="15" />生成</button></div>
          <div class="tool-output whitespace-pre-wrap" :class="promptResult ? 'text-slate-700' : 'text-slate-400'">{{ promptResult || '输入主题后生成说明' }}</div>
        </section>
        <section class="tool-panel">
          <div class="tool-heading"><Braces :size="17" class="text-emerald-500" /><div><h2>结构化信息</h2><p>将常见问题整理为可读取的数据</p></div></div>
          <div class="mt-4 grid gap-2 sm:grid-cols-[10rem_1fr_auto]"><select v-model="structuredType" class="tool-input"><option value="actor">演员作品</option><option value="language">语言信息</option><option value="cities">城市列表</option></select><input v-model="structuredInput" class="tool-input" :placeholder="structuredPlaceholder" @keydown.enter="runStructured" /><button class="tool-button bg-emerald-600" :disabled="busy === 'structured'" @click="runStructured"><LoaderCircle v-if="busy === 'structured'" class="animate-spin" :size="15" /><Play v-else :size="15" />整理</button></div>
          <pre class="tool-output" :class="structuredResult ? 'text-slate-700' : 'text-slate-400'">{{ structuredResult || '选择类型并输入内容后整理' }}</pre>
        </section>
        <section class="tool-panel">
          <div class="tool-heading"><Search :size="17" class="text-amber-500" /><div><h2>联网问答</h2><p>先搜索最新资料，再生成回答</p></div></div>
          <div class="mt-4 flex gap-2"><input v-model="advisorQuestion" class="tool-input" placeholder="例如：Spring AI 最新版本" @keydown.enter="runAdvisor" /><button class="tool-button bg-amber-500" :disabled="busy === 'advisor'" @click="runAdvisor"><LoaderCircle v-if="busy === 'advisor'" class="animate-spin" :size="15" /><Radio v-else :size="15" />回答</button></div>
          <div class="tool-output whitespace-pre-wrap" :class="advisorResult ? 'text-slate-700' : 'text-slate-400'">{{ advisorResult || '输入问题后获取实时回答' }}</div>
        </section>
        <section class="tool-panel">
          <div class="tool-heading"><BookOpen :size="17" class="text-rose-500" /><div><h2>知识库客服</h2><p>基于已上传资料回答，并记录执行指标</p></div></div>
          <div class="mt-4 flex gap-2"><input v-model="agentQuestion" class="tool-input" placeholder="例如：如何接入 Ollama？" @keydown.enter="runAgent" /><button class="tool-button bg-rose-500" :disabled="busy === 'agent'" @click="runAgent"><LoaderCircle v-if="busy === 'agent'" class="animate-spin" :size="15" /><MessageCircle v-else :size="15" />回答</button></div>
          <div v-if="agentResult" class="mt-4 rounded-lg border border-slate-100 bg-slate-50 p-3 text-sm leading-6 text-slate-700 whitespace-pre-wrap">{{ agentResult.answer || '知识库没有返回内容' }}</div><div v-else class="tool-output text-slate-400">上传知识库文件后，可以在这里直接提问</div>
          <div v-if="agentResult" class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-slate-400"><span>检索 {{ agentResult.metrics?.retrievalCalls ?? 0 }} 次</span><span>上下文 {{ agentResult.metrics?.contextChars ?? 0 }} 字符</span><span>耗时 {{ agentResult.metrics?.totalMs ?? 0 }} ms</span><button class="ml-auto text-rose-500 hover:text-rose-700" @click="compareAgent">查看基线对比</button></div>
          <div v-if="comparison" class="mt-3 rounded-lg border border-dashed border-slate-200 p-3 text-xs text-slate-500">Harness 比基线 {{ comparison.latencyDeltaMs <= 0 ? '快' : '慢' }} {{ Math.abs(comparison.latencyDeltaMs) }} ms，模型调用差值 {{ comparison.modelCallDelta }} 次。</div>
        </section>
      </main>
    </div>
  </Layout>
</template>
<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, Braces, LoaderCircle, MessageCircle, Play, Radio, Search, TextCursorInput, WandSparkles } from 'lucide-vue-next'
import Layout from '@/layouts/Layout.vue'
import { api } from '@/services/api'
const router = useRouter()
const busy = ref('')
const topic = ref('Spring AI')
const promptResult = ref('')
const structuredType = ref('actor')
const structuredInput = ref('周星驰')
const structuredResult = ref('')
const advisorQuestion = ref('Spring AI 最新版本')
const advisorResult = ref('')
const agentQuestion = ref('如何接入 Ollama？')
const agentResult = ref(null)
const comparison = ref(null)
const structuredPlaceholder = computed(() => ({ actor: '例如：周星驰', language: '例如：Java', cities: '例如：中国' }[structuredType.value]))
async function runPrompt() { if (!topic.value.trim()) return; busy.value = 'prompt'; promptResult.value = ''; try { promptResult.value = await api.promptTemplate(topic.value.trim()) } catch (error) { promptResult.value = `请求失败：${error.message}` } finally { busy.value = '' } }
async function runStructured() { if (!structuredInput.value.trim()) return; busy.value = 'structured'; structuredResult.value = ''; try { const value = structuredType.value === 'actor' ? await api.structuredActor(structuredInput.value.trim()) : structuredType.value === 'language' ? await api.structuredLanguage(structuredInput.value.trim()) : await api.structuredCities(structuredInput.value.trim()); structuredResult.value = JSON.stringify(value, null, 2) } catch (error) { structuredResult.value = `请求失败：${error.message}` } finally { busy.value = '' } }
async function runAdvisor() { if (!advisorQuestion.value.trim()) return; busy.value = 'advisor'; advisorResult.value = ''; try { await api.streamAdvisor(advisorQuestion.value.trim(), { onChunk(chunk) { if (chunk.v) advisorResult.value += chunk.v; if (chunk.reasoning) advisorResult.value += chunk.reasoning } }) } catch (error) { advisorResult.value = `请求失败：${error.message}` } finally { busy.value = '' } }
async function runAgent() { if (!agentQuestion.value.trim()) return; busy.value = 'agent'; comparison.value = null; try { agentResult.value = await api.agentRun(agentQuestion.value.trim(), 'harness') } catch (error) { agentResult.value = { answer: `请求失败：${error.message}` } } finally { busy.value = '' } }
async function compareAgent() { if (!agentQuestion.value.trim() || busy.value) return; busy.value = 'agent'; try { comparison.value = await api.agentCompare(agentQuestion.value.trim()) } catch (error) { comparison.value = null; agentResult.value = { ...agentResult.value, answer: `对比失败：${error.message}` } } finally { busy.value = '' } }
</script>
<style scoped>
.tool-panel { border: 1px solid rgb(226 232 240); border-radius: 0.75rem; background: white; padding: 1.25rem; box-shadow: 0 1px 2px rgb(15 23 42 / 0.05); }
.tool-heading { display: flex; align-items: flex-start; gap: 0.5rem; font-size: 0.875rem; font-weight: 600; color: rgb(30 41 59); }
.tool-heading h2 { font-weight: 600; color: rgb(30 41 59); }
.tool-heading p { margin-top: 0.25rem; font-size: 0.75rem; font-weight: 400; color: rgb(148 163 184); }
.tool-input { min-width: 0; flex: 1 1 0%; border: 1px solid rgb(226 232 240); border-radius: 0.5rem; background: white; padding: 0.5rem 0.75rem; font-size: 0.875rem; color: rgb(51 65 85); outline: none; transition: border-color 150ms; }
.tool-input:focus { border-color: rgb(129 140 248); }
.tool-button { display: inline-flex; flex-shrink: 0; align-items: center; justify-content: center; gap: 0.375rem; border-radius: 0.5rem; padding: 0.5rem 0.75rem; font-size: 0.75rem; color: white; transition: filter 150ms; }
.tool-button:hover { filter: brightness(0.95); }
.tool-button:disabled { cursor: not-allowed; opacity: 0.5; }
.tool-output { margin-top: 1rem; min-height: 6rem; max-height: 13rem; overflow: auto; border-radius: 0.5rem; background: rgb(248 250 252); padding: 0.75rem; font-size: 0.75rem; line-height: 1.5rem; }
</style>