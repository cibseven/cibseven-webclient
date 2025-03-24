import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'All UI elements',
      component: () => import('../components/GroupAll.vue'),
    },
    {
      path: '/avatars',
      name: 'Avatars',
      component: () => import('../components/GroupAvatars.vue'),
    },
    {
      path: '/lists',
      name: 'Lists',
      component: () => import('../components/GroupLists.vue'),
    },
    {
      path: '/buttons',
      name: 'Buttons',
      component: () => import('../components/GroupButtons.vue'),
    },
    {
      path: '/tabs',
      name: 'Tabs',
      component: () => import('../components/GroupTabs.vue'),
    },
    {
      path: '/dialogs',
      name: 'Dialogs',
      component: () => import('../components/GroupDialogs.vue'),
    },
    {
      path: '/alerts',
      name: 'Alerts',
      component: () => import('../components/GroupAlerts.vue'),
    },
    {
      path: '/progress',
      name: 'Progress',
      component: () => import('../components/GroupProgress.vue'),
    },
    {
      path: '/waiting-box',
      name: 'WaitingBox',
      component: () => import('../components/GroupWaitingBox.vue'),
    },
  ],
})

export default router
