<template>
  <Layout @home="router.push('/')" @customer-service="router.push('/customer-service')" @lab="router.push('/lab')" @select-chat="id => router.push('/chat/' + id)" @new-chat="router.push('/')">
    <div class="flex h-full flex-col">
      <header class="flex h-16 shrink-0 items-center justify-between border-b border-slate-200/70 bg-white/70 px-6 backdrop-blur"><div class="flex items-center gap-2 text-sm font-medium text-slate-700"><MessageSquare :size="16" class="text-[#4d6bfe]" />{{ title }}</div><span class="text-xs text-slate-400">{{ selectedModel }}</span></header>
      <div ref="scroll" class="scroll-soft min-h-0 flex-1 overflow-y-auto"><div class="mx-auto max-w-3xl px-4 py-8 sm:px-8"><div v-if="!messages.length" class="py-20 text-center text-sm text-slate-400">开始一段新的对话</div><article v-for="(item, index) in messages" :key="index" class="mb-7 flex gap-3" :class="item.role === 'user' ? 'justify-end' : 'justify-start'"><div v-if="item.role !== 'user'" class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-[#eef1ff] text-[#4d6bfe]"><Sparkles :size="16" /></div><div :class="item.role === 'user' ? 'bg-[#edf2ff] text-slate-800' : 'bg-white text-slate-700'" class="max-w-[84%] rounded-2xl px-4 py-3 text-[15px] leading-7 shadow-sm"><div v-if="item.reasoning" class="mb-2 border-b border-slate-100 pb-2 text-xs text-slate-400"><span class="mr-1 font-medium text-slate-500">推理过程</span>{{ item.reasoning }}</div><MarkdownRenderer v-if="item.role !== 'user'" :content="item.content" /><span v-else class="whitespace-pre-wrap">{{ item.content }}</span><LoadingSpinner v-if="item.role !== 'user' && index === messages.length - 1 && busy" /></div></article></div></div>
      <div class="shrink-0 border-t border-slate-200/70 bg-[#f7f9fc]/95 pt-3"><ChatInputBox :busy="busy" @send="sendMessage" /></div>
    </div>
  </Layout>
</template>
<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessageSquare, Sparkles } from 'lucide-vue-next'
import Layout from '@/layouts/Layout.vue'
import ChatInputBox from '@/components/ChatInputBox.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { api } from '@/services/api'
const route = useRoute(); const router = useRouter(); const chatId = route.params.chatId; const messages = ref([]); const busy = ref(false); const scroll = ref(null); const selectedModel = ref('Qwen3 本地')
const title = computed(() => messages.value.find(m => m.role === 'user')?.content?.slice(0, 28) || '新对话')
async function loadMessages() { try { const result = await api.messages(chatId); messages.value = (result.data || []).map(m => ({ role: m.role, content: m.content || '', reasoning: m.reasoning || '' })) } catch {} }
function bottom() { nextTick(() => { if (scroll.value) scroll.value.scrollTop = scroll.value.scrollHeight }) }
async function sendMessage(payload) {
  if (!payload?.message || busy.value) return
  selectedModel.value = payload.selectedModel?.name || 'Qwen3 本地'; const user = payload.message
  messages.value.push({ role: 'user', content: user }); messages.value.push({ role: 'assistant', content: '', reasoning: '' }); busy.value = true; bottom()
  try {
    await api.streamChat({ message: user, chatId, modelName: payload.selectedModel?.name, networkSearch: payload.isNetworkSearch, temperature: 0.7 }, {
      onChunk(chunk) { const last = messages.value[messages.value.length - 1]; if (chunk.v) last.content += chunk.v; if (chunk.reasoning) last.reasoning += chunk.reasoning; if (chunk.done) busy.value = false; bottom() },
      onClose() { busy.value = false }, onError(error) { throw error }
    })
  } catch (error) { messages.value[messages.value.length - 1].content = `请求失败：${error.message}` } finally { busy.value = false; bottom() }
}
onMounted(async () => { await loadMessages(); const first = route.query.first; if (first && !messages.value.length) await sendMessage({ message: first, selectedModel: { name: 'qwen3:1.7b' }, isNetworkSearch: false }) })
</script>
