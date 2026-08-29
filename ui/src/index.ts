import { definePlugin } from '@halo-dev/console-shared'
import ActivityList from './views/ActivityList.vue'
import ActivityEditor from './views/ActivityEditor.vue'
import RegistrationList from './views/RegistrationList.vue'
import { IconPlug } from '@halo-dev/components'
import { markRaw } from 'vue'

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
