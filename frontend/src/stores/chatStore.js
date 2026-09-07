import { defineStore } from 'pinia'

export const useChatStore = defineStore('chat', {
  state: () => ({
    selectedModel: { name: 'qwen3:1.7b', label: 'Qwen3 1.7B 本地模型' },
    isNetworkSearchSelected: false,
    chats: [],
    chatsPage: 1,
    chatsHasMore: true
  }),
  actions: {
    setModel(model) { this.selectedModel = model },
    setNetworkSearch(value) { this.isNetworkSearchSelected = value },
    resetChats() { this.chats = []; this.chatsPage = 1; this.chatsHasMore = true }
  },
  persist: {
    key: 'zhida-chat-settings',
    paths: ['selectedModel', 'isNetworkSearchSelected']
  }
})
