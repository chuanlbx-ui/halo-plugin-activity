import { definePlugin } from '@halo-dev/console-shared'
import ActivityList from './views/ActivityList.vue'
import ActivityEditor from './views/ActivityEditor.vue'
import RegistrationList from './views/RegistrationList.vue'
import { IconPlug } from '@halo-dev/components'
import { markRaw } from 'vue'
// 注意：Halo 控制台只加载插件的 main.js（IIFE），不会自动加载 style.css。
// 因此所有 CSS 必须通过 ?inline 导入并手动注入 <style> 标签，否则样式完全丢失。
import richtextCss from '@halo-dev/richtext-editor/dist/style.css?inline'
import tailwindCss from './assets/tailwind.css?inline'
import pluginCss from './assets/plugin.css?inline'

if (typeof document !== 'undefined') {
  const styleEl = document.createElement('style')
  styleEl.setAttribute('data-activity-plugin', 'true')
  styleEl.textContent = [richtextCss, tailwindCss, pluginCss].join('\n')
  document.head.appendChild(styleEl)
}

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/activities',
        name: 'Activities',
        component: ActivityList,
        meta: {
          title: '活动列表',
          searchable: true,
          menu: {
            name: '活动管理',
            group: '内容',
            icon: markRaw(IconPlug),
            priority: 0,
          },
        },
      },
    },
    {
      parentName: 'Root',
      route: {
        path: '/activities/create',
        name: 'ActivityCreate',
        component: ActivityEditor,
        meta: {
          title: '新建活动',
          searchable: true,
        },
      },
    },
    {
      parentName: 'Root',
      route: {
        path: '/activities/:name/edit',
        name: 'ActivityEdit',
        component: ActivityEditor,
        meta: {
          title: '编辑活动',
          searchable: true,
        },
      },
    },
    {
      parentName: 'Root',
      route: {
        path: '/activities/:name/registrations',
        name: 'ActivityRegistrations',
        component: RegistrationList,
        meta: {
          title: '报名记录',
          searchable: true,
        },
      },
    },
  ],
  extensionPoints: {},
})
