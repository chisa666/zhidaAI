<template>
  <div class="w-full px-4 pb-3 sm:px-8">
    <div class="mx-auto max-w-3xl rounded-2xl border border-slate-200 bg-white p-3 shadow-[0_8px_30px_rgba(42,57,92,.06)]">
      <textarea ref="input" v-model="draft" @input="resize" @keydown.enter.exact.prevent="submit" :placeholder="placeholder" rows="1" class="max-h-[180px] min-h-[28px] w-full resize-none border-0 bg-transparent px-1 text-[15px] leading-7 text-slate-800 outline-none placeholder:text-slate-400"></textarea>
      <div class="mt-1 flex items-center justify-between gap-2"><div class="flex items-center gap-2"><select v-model="model" class="rounded-lg border-0 bg-slate-50 px-2 py-1.5 text-xs text-slate-600 outline-none"><option value="qwen3:1.7b">Qwen3 1.7B 本地</option></select><button @click="network = !network" :class="network ? 'bg-indigo-50 text-indigo-600' : 'text-slate-400 hover:bg-slate-50'" class="flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs" title="联网搜索"><Search :size="14" />联网</button></div><button @click="submit" :disabled="!draft.trim() || busy" class="grid h-8 w-8 place-items-center rounded-full bg-[#4d6bfe] text-white transition hover:bg-[#3c57de] disabled:cursor-not-allowed disabled:opacity-40" title="发送"><Send :size="15" /></button></div>
    </div><div class="mt-2 text-center text-[11px] text-slate-400">内容由 AI 生成，请仔细甄别</div>
  </div>
</template>
<script setup>
import { ref, nextTick, computed } from 'vue'; import { Search, Send } from 'lucide-vue-next'; import { useChatStore } from '@/stores/chatStore'
const props = defineProps({ modelValue: { type: String, default: '' }, placeholder: { type: String, default: '给智答ai发送消息' }, busy: Boolean }); const emit = defineEmits(['update:modelValue','send'])
const store = useChatStore(); const input = ref(null); const draft = ref(props.modelValue)
const model = computed({ get: () => store.selectedModel.name, set: value => store.setModel({ name: value, label: 'Qwen3 1.7B local model' }) })
const network = computed({ get: () => store.isNetworkSearchSelected, set: value => store.setNetworkSearch(value) })
function resize() { if (!input.value) return; input.value.style.height = 'auto'; input.value.style.height = `${Math.min(input.value.scrollHeight, 180)}px`; emit('update:modelValue', draft.value) }
function submit() { if (!draft.value.trim() || props.busy) return; emit('send', { message: draft.value.trim(), selectedModel: { name: model.value }, isNetworkSearch: network.value }); draft.value = ''; nextTick(resize) }
</script>
