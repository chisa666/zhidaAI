<template>
  <section class="rounded-xl border border-slate-200 bg-slate-50/70 p-4">
    <div class="flex items-center justify-between gap-3">
      <div>
        <div class="text-sm font-medium text-slate-700">大文件分片上传</div>
        <div class="mt-1 text-xs text-slate-400">支持 MD5 秒传、断点续传、并发上传和失败重试</div>
      </div>
      <label class="shrink-0 cursor-pointer rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600 hover:border-indigo-300 hover:text-indigo-600">
        选择文件
        <input type="file" class="hidden" :disabled="busy" @change="choose" />
      </label>
    </div>
    <div v-if="file" class="mt-4 space-y-2">
      <div class="flex items-center justify-between gap-3 text-xs text-slate-600">
        <span class="truncate" :title="file.name">{{ file.name }}</span>
        <span class="shrink-0 text-slate-400">{{ formatSize(file.size) }}</span>
      </div>
      <div class="h-2 overflow-hidden rounded-full bg-slate-200"><div class="h-full rounded-full bg-[#4d6bfe] transition-all" :style="{ width: `${progress}%` }"></div></div>
      <div class="flex items-center justify-between text-[11px] text-slate-400"><span>{{ stage }}</span><span>{{ progress }}%</span></div>
      <p v-if="error" class="text-xs text-red-500">{{ error }}</p>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import SparkMD5 from 'spark-md5'
import { api } from '@/services/api'

const CHUNK_SIZE = 5 * 1024 * 1024
const CONCURRENCY = 3
const MAX_RETRIES = 3
const emit = defineEmits(['completed'])
const file = ref(null); const progress = ref(0); const stage = ref('等待选择文件'); const busy = ref(false); const error = ref('')

function formatSize(value) { if (value < 1024) return `${value} B`; if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`; return `${(value / 1024 / 1024).toFixed(1)} MB` }
async function digest(target) {
  const md5 = new SparkMD5.ArrayBuffer(); const total = Math.max(1, Math.ceil(target.size / CHUNK_SIZE))
  for (let index = 0; index < total; index++) { md5.append(await target.slice(index * CHUNK_SIZE, Math.min(target.size, (index + 1) * CHUNK_SIZE)).arrayBuffer()); progress.value = Math.min(20, Math.round((index + 1) / total * 20)); stage.value = `计算文件指纹 ${progress.value}%` }
  return md5.end()
}
function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)) }
async function uploadOne(md5, index, target, total, onDone) {
  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    try { await api.uploadChunk(md5, index, target.slice(index * CHUNK_SIZE, Math.min(target.size, (index + 1) * CHUNK_SIZE))); onDone(); return } catch (cause) { if (attempt === MAX_RETRIES) throw cause; await sleep(500 * 2 ** attempt) }
  }
}
async function run(target) {
  const md5 = await digest(target); const chunkCount = Math.max(1, Math.ceil(target.size / CHUNK_SIZE));
  const checked = await api.checkFile(md5, target.name, target.size, chunkCount); const result = checked.data || {}
  if (result.fastUpload) { progress.value = 100; stage.value = '秒传完成'; emit('completed', result); return }
  const uploaded = new Set(result.uploadedChunkIndexes || []); let completed = uploaded.size; progress.value = 20 + Math.round(completed / chunkCount * 70); stage.value = `上传分片（已完成 ${completed}/${chunkCount}）`
  const pending = Array.from({ length: chunkCount }, (_, index) => index).filter(index => !uploaded.has(index)); let cursor = 0
  const worker = async () => { while (cursor < pending.length) { const index = pending[cursor++]; await uploadOne(md5, index, target, chunkCount, () => { completed++; progress.value = 20 + Math.round(completed / chunkCount * 70); stage.value = `上传分片（已完成 ${completed}/${chunkCount}）` }) } }
  await Promise.all(Array.from({ length: Math.min(CONCURRENCY, pending.length) }, worker)); stage.value = '正在合并文件'; const merged = await api.mergeFile(md5, target.name, chunkCount); progress.value = 100; stage.value = '上传完成'; emit('completed', merged.data || merged)
}
async function choose(event) { const selected = event.target.files?.[0]; event.target.value = ''; if (!selected) return; file.value = selected; progress.value = 0; error.value = ''; busy.value = true; stage.value = '准备上传'; try { await run(selected) } catch (cause) { error.value = cause?.message || '上传失败'; stage.value = '上传失败' } finally { busy.value = false } }
</script>
