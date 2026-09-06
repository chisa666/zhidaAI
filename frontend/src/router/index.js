import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/views/HomePage.vue'
import ChatPage from '@/views/ChatPage.vue'
import CustomerServicePage from '@/views/CustomerServicePage.vue'
import CapabilityLabPage from '@/views/CapabilityLabPage.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomePage },
    { path: '/chat/:chatId', component: ChatPage },
    { path: '/customer-service', component: CustomerServicePage },
    { path: '/lab', component: CapabilityLabPage }
  ]
})
