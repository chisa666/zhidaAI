<template>
  <aside :class="[open ? 'w-72' : 'w-0', open ? 'translate-x-0' : '-translate-x-full']" class="fixed inset-y-0 left-0 z-30 flex flex-col overflow-hidden border-r border-slate-200 bg-[#f9fbff] transition-all duration-200 lg:relative lg:translate-x-0">
    <div class="flex min-w-72 items-center gap-3 px-5 py-5">
      <div class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-[#4d6bfe] text-white shadow-sm"><Sparkles :size="20" /></div>
      <div><div class="text-[17px] font-semibold tracking-wide text-slate-900">智答ai</div><div class="text-[11px] text-slate-400">by chisa</div></div>
    </div>
    <div class="min-w-72 px-4">
      <button @click="$emit('new-chat')" class="flex w-full items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 shadow-sm transition hover:border-[#9cacff] hover:text-[#415ad8]"><Plus :size="17" />开启新对话</button>
      <nav class="mt-5 space-y-1">
        <button @click="$emit('home')" class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-white hover:text-slate-900"><MessageSquare :size="16" />聊天</button>
        <button @click="$emit('customer-service')" class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-white hover:text-slate-900"><BookOpen :size="16" />智能客服知识库</button>
        <button @click="$emit('lab')" class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-white hover:text-slate-900"><FlaskConical :size="16" />能力实验室</button>
      </nav>
    </div>
    <div class="mt-6 flex min-h-0 min-w-72 flex-1 flex-col px-4">
      <div class="mb-2 flex items-center justify-between px-2 text-[11px] font-medium uppercase tracking-wider text-slate-400"><span>历史对话</span><button @click="loadChats" title="刷新" class="text-slate-400 hover:text-slate-700">↻</button></div>
      <div class="scroll-soft min-h-0 flex-1 space-y-0.5 overflow-y-auto">
        <button v-for="chat in store.chats" :key="chat.uuid" @click="$emit('select-chat', chat.uuid)" :class="route.params.chatId === chat.uuid ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600'" class="group flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-[13px] hover:bg-white"><span class="truncate">{{ chat.summary || '新对话' }}</span><span class="hidden gap-1 group-hover:flex"><button @click.stop="rename(chat)" title="重命名" class="p-0.5 text-slate-400 hover:text-[#4d6bfe]"><Pencil :size="13" /></button><button @click.stop="remove(chat)" title="删除" class="p-0.5 text-slate-400 hover:text-red-500"><Trash2 :size="13" /></button></span></button>
        <div v-if="!store.chats.length" class="px-3 py-5 text-center text-xs text-slate-400">还没有历史对话</div>
      </div>
    </div>
    <div class="min-w-72 border-t border-slate-200 px-5 py-4 text-xs text-slate-400">智答ai · Spring AI 实战</div>
  </aside>
  <button @click="open = !open" :class="open ? 'left-64 lg:left-72' : 'left-0'" class="fixed top-4 z-40 rounded-r-lg border border-slate-200 bg-white p-2 text-slate-500 shadow-sm transition-all hover:text-slate-900 lg:absolute" :title="open ? '收起侧栏' : '展开侧栏'"><PanelLeftClose v-if="open" :size="18" /><PanelLeftOpen v-else :size="18" /></button>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Sparkles, MessageSquare, BookOpen, Plus, Pencil, Trash2, PanelLeftClose, PanelLeftOpen, FlaskConical } from 'lucide-vue-next'
import { useChatStore } from '@/stores/chatStore'
import { api } from '@/services/api'
const store = useChatStore(); const route = useRoute(); const router = useRouter(); const open = ref(true)
async function loadChats() { try { const res = await api.chatHistory(1, 50); store.chats = res.data || [] } catch {} }
async function remove(chat) { await api.deleteChat(chat.uuid); if (route.params.chatId === chat.uuid) router.push('/'); await loadChats() }
async function rename(chat) { const summary = window.prompt('请输入对话名称', chat.summary); if (summary?.trim()) { await api.renameChat(chat.uuid, summary.trim()); await loadChats() } }
onMounted(loadChats)
</script>
