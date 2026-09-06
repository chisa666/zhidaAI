import { fetchEventSource } from '@microsoft/fetch-event-source'

export const api = {
  async request(path, options = {}) {
    const isForm = options.body instanceof FormData
    const headers = { ...(isForm ? {} : { 'Content-Type': 'application/json' }), ...(options.headers || {}) }
    if (isForm) delete headers['Content-Type']
    const response = await fetch(path, { headers, ...options })
    const data = await response.json()
    if (!response.ok || data.success === false) throw new Error(data.message || '请求失败')
    return data
  },
  newChat(message) { return this.request('/api/chat/new', { method: 'POST', body: JSON.stringify({ message }) }) },
  chatHistory(current = 1, size = 30) { return this.request(`/api/chat/history?current=${current}&size=${size}`) },
  messages(chatId, current = 1, size = 100) { return this.request(`/api/chat/${encodeURIComponent(chatId)}/messages?current=${current}&size=${size}`) },
  renameChat(chatId, summary) { return this.request('/api/chat/rename', { method: 'POST', body: JSON.stringify({ chatId, summary }) }) },
  deleteChat(chatId) { return this.request('/api/chat/delete', { method: 'POST', body: JSON.stringify({ chatId }) }) },
  files(current = 1, size = 20, keyword = '') { return this.request(`/api/customer-service/md/list?current=${current}&size=${size}&keyword=${encodeURIComponent(keyword)}`) },
  async uploadKnowledge(file, remark = '') { const body = new FormData(); body.append('file', file); body.append('remark', remark); return this.request('/api/customer-service/md/upload', { method: 'POST', body }) },
  updateFile(id, fileName, remark) { return this.request(`/api/customer-service/md/${id}`, { method: 'PUT', body: JSON.stringify({ id, fileName, remark }) }) },
  deleteFile(id) { return this.request(`/api/customer-service/md/${id}`, { method: 'DELETE' }) },
  checkFile(fileMd5, fileName, fileSize, chunkCount) {
    return this.request('/api/file/check', { method: 'POST', body: JSON.stringify({ md5: fileMd5, fileName, fileSize, chunkCount }) })
  },
  uploadChunk(fileMd5, chunkIndex, chunk) {
    const body = new FormData()
    body.append('md5', fileMd5)
    body.append('chunkIndex', String(chunkIndex))
    body.append('chunk', chunk, `${chunkIndex}.part`)
    return this.request('/api/file/chunk', { method: 'POST', body })
  },
  mergeFile(fileMd5, fileName, chunkCount) {
    return this.request('/api/file/merge', { method: 'POST', body: JSON.stringify({ md5: fileMd5, fileName, chunkCount }) })
  },
  promptTemplate(topic = 'Java') { return this.request(`/api/lab/prompt/template?topic=${encodeURIComponent(topic)}`) },
  promptRole(question) { return this.request(`/api/lab/prompt/role?question=${encodeURIComponent(question)}`) },
  structuredActor(name = '周星驰') { return this.request(`/api/lab/structured/actor-films?name=${encodeURIComponent(name)}`) },
  structuredLanguage(language = 'Java') { return this.request(`/api/lab/structured/language-info?language=${encodeURIComponent(language)}`) },
  structuredCities(country = '中国') { return this.request(`/api/lab/structured/city-list?country=${encodeURIComponent(country)}`) },
  agentRun(question, strategy = 'harness') { return this.request(`/api/agent/support-agent/run?question=${encodeURIComponent(question)}&strategy=${strategy}`) },
  agentCompare(question) { return this.request(`/api/agent/support-agent/compare?question=${encodeURIComponent(question)}`) },
  async streamAdvisor(message, handlers = {}) {
    await fetchEventSource(`/api/lab/advisor/network/generateStream?message=${encodeURIComponent(message)}`, {
      headers: { Accept: 'text/event-stream' },
      onmessage(event) { if (!event.data) return; try { handlers.onChunk?.(JSON.parse(event.data)) } catch {} },
      onclose() { handlers.onClose?.() }, onerror(error) { handlers.onError?.(error); throw error }
    })
  },
  async streamChat(payload, handlers = {}) {
    const controller = new AbortController()
    await fetchEventSource('/api/chat/completion', {
      method: 'POST', signal: controller.signal,
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
      body: JSON.stringify(payload),
      onmessage(event) {
        if (!event.data) return
        try { handlers.onChunk?.(JSON.parse(event.data)) } catch { /* ignore incomplete event */ }
      },
      onclose() { handlers.onClose?.() },
      onerror(error) { handlers.onError?.(error); throw error }
    })
    return controller
  },
  async streamCustomerChat(payload, handlers = {}) {
    await fetchEventSource('/api/customer-service/chat/completion', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' }, body: JSON.stringify(payload),
      onmessage(event) { if (!event.data) return; try { handlers.onChunk?.(JSON.parse(event.data)) } catch {} },
      onclose() { handlers.onClose?.() }, onerror(error) { handlers.onError?.(error); throw error }
    })
  }
}
