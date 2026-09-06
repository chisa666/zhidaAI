<template><component :is="icons[name] || icons.sparkles" :class="customCss" :size="size" /></template>
<script setup>
import { Sparkles, MessageSquare, PanelLeftClose, PanelLeftOpen, Plus, Search, Send, Settings2, BookOpen, Trash2, Pencil, Upload, FileText } from 'lucide-vue-next'
defineProps({ name: { type: String, default: 'sparkles' }, customCss: { type: String, default: '' }, size: { type: [String, Number], default: 18 } })
const icons = { sparkles: Sparkles, message: MessageSquare, 'sidebar-open': PanelLeftClose, 'sidebar-close': PanelLeftOpen, plus: Plus, search: Search, send: Send, settings: Settings2, book: BookOpen, trash: Trash2, pencil: Pencil, upload: Upload, file: FileText }
</script>
