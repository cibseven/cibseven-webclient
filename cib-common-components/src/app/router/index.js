/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
    {
      path: '/forms',
      name: 'Forms',
      component: () => import('../components/GroupForm.vue'),
    },
  ],
})

export default router
