<template>
  <div class="markdown-body" v-html="html" @click="copyCode"></div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { marked } from 'marked'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github.css'

const props = defineProps({ content: { type: String, default: '' } })
const codeBlocks = ref([])
const renderer = new marked.Renderer()
renderer.code = ({ text, lang }) => {
  const index = codeBlocks.value.push(text) - 1
  const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
  const highlighted = language === 'plaintext'
    ? hljs.highlightAuto(text).value
    : hljs.highlight(text, { language }).value
  return '<div class="relative my-3 overflow-hidden rounded-lg border border-slate-200 bg-slate-50">' +
    '<div class="flex items-center justify-between border-b border-slate-200 px-3 py-1.5 text-xs text-slate-500">' +
    '<span>' + language + '</span><button type="button" data-code-index="' + index + '" class="rounded px-2 py-1 hover:bg-white">Copy</button></div>' +
    '<pre class="m-0 overflow-x-auto p-3 text-xs leading-5"><code class="hljs language-' + language + '">' + highlighted + '</code></pre></div>'
}
marked.setOptions({ breaks: true, gfm: true, renderer })
const html = computed(() => {
  codeBlocks.value = []
  return marked.parse(props.content || '')
})
function copyCode(event) {
  const button = event.target.closest?.('[data-code-index]')
  if (!button) return
  const code = codeBlocks.value[Number(button.dataset.codeIndex)]
  if (code == null) return
  navigator.clipboard.writeText(code).then(() => message.success('Copied'))
}
</script>
