<template>
  <Layout @home="router.push('/')" @customer-service="router.push('/customer-service')" @lab="router.push('/lab')" @select-chat="id => router.push('/chat/' + id)" @new-chat="focusInput">
    <div class="flex h-full flex-col">
      <header class="flex h-16 items-center justify-end border-b border-slate-200/70 px-6"><span class="text-xs text-slate-400">本地模型工作台</span></header>
      <section class="flex flex-1 flex-col items-center justify-center px-5 pb-12">
        <div class="mb-6 grid h-16 w-16 place-items-center rounded-2xl bg-[#4d6bfe] text-white shadow-[0_10px_24px_rgba(77,107,254,.2)]"><Sparkles :size="32" /></div>
        <h1 class="text-3xl font-semibold tracking-tight text-slate-900 sm:text-4xl">你好，我是智答ai</h1>
        <p class="mt-3 text-center text-sm text-slate-500">由 chisa 构建的 Spring AI 智能问答工作台</p>
        <div class="mt-9 grid w-full max-w-2xl grid-cols-1 gap-3 sm:grid-cols-3"><button v-for="item in suggestions" :key="item" @click="quickAsk(item)" class="rounded-xl border border-slate-200 bg-white p-4 text-left text-sm text-slate-600 shadow-sm transition hover:-translate-y-0.5 hover:border-[#a8b4ff] hover:text-slate-900">{{ item }}</button></div>
      </section>
      <ChatInputBox ref="chatInput" :busy="busy" @send="sendMessage" />
    </div>
  </Layout>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Sparkles } from 'lucide-vue-next'
import Layout from '@/layouts/Layout.vue'
import ChatInputBox from '@/components/ChatInputBox.vue'
import { api } from '@/services/api'
const router = useRouter(); const busy = ref(false); const chatInput = ref(null)
const suggestions = ['帮我制定一个学习计划', 'Spring AI 可以做什么？', '解释一下 RAG 增强检索']
async function sendMessage(payload) { if (!payload.message || busy.value) return; busy.value = true; try { const res = await api.newChat(payload.message); router.push({ path: `/chat/${res.data.uuid}`, query: { first: payload.message, model: payload.selectedModel?.name || 'qwen3:1.7b', network: payload.isNetworkSearch ? '1' : '0' } }) } catch (error) { window.alert(error.message) } finally { busy.value = false } }
function quickAsk(message) { sendMessage({ message }) }
function focusInput() { chatInput.value?.$el?.querySelector('textarea')?.focus() }
</script>
