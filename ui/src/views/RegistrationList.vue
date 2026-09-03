<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  VButton,
  VCard,
  VEmpty,
  VEntity,
  VEntityField,
  VLoading,
  VPageHeader,
  VPagination,
  VStatusDot,
  Toast,
} from '@halo-dev/components'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const activityName = String(route.params.name)

const loading = ref(false)
const activity = ref<any>(null)
const registrations = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const API_BASE = '/apis/console.api.activity.halo.run/v1alpha1'

const countText = computed(() => {
  const quota = activity.value?.spec?.quota
  if (quota && quota > 0) {
    return `${total.value} / ${quota}`
  }
  return String(total.value)
})

async function loadActivity() {
  try {
    const { data } = await axios.get(`${API_BASE}/activities/${activityName}`)
    activity.value = data
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '加载活动失败')
  }
}

async function fetchRegistrations() {
  loading.value = true
  try {
    const { data } = await axios.get(`${API_BASE}/activities/${activityName}/registrations`, {
      params: { page: page.value, size: size.value },
    })
    registrations.value = data.items || []
    total.value = data.total || 0
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '加载报名记录失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(value: { page: number; size: number }) {
  page.value = value.page
  fetchRegistrations()
}

function formatTime(iso?: string) {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

function statusText(status?: string) {
  const map: Record<string, { text: string; type: 'success' | 'warning' | 'error' | 'default' }> = {
    APPROVED: { text: '已报名', type: 'success' },
    PENDING: { text: '待确认', type: 'warning' },
    REJECTED: { text: '已拒绝', type: 'error' },
    CANCELLED: { text: '已取消', type: 'default' },
  }
  return map[status || 'PENDING'] || { text: status || '未知', type: 'default' }
}

async function toggleCheckin(reg: any) {
  const name = reg.metadata?.name
  const checkedIn = reg.spec?.checkedIn
  try {
    const action = checkedIn ? 'uncheckin' : 'checkin'
    const { data } = await axios.post(`${API_BASE}/registrations/${name}/${action}`)
    const idx = registrations.value.findIndex(r => r.metadata?.name === name)
    if (idx >= 0) {
      registrations.value[idx] = data
    }
    Toast.success(checkedIn ? '已取消签到' : '✅ 签到成功')
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  loadActivity()
  fetchRegistrations()
})
</script>

<template>
  <div>
    <VPageHeader :title="`报名记录 · ${activity?.spec?.title || activityName}`">
      <template #actions>
        <VButton type="secondary" @click="router.push({ name: 'Activities' })">返回列表</VButton>
      </template>
    </VPageHeader>

    <div class="p-4">
      <VCard :body-class="['p-4']">
        <div class="mb-4 flex items-center justify-between">
          <div class="text-sm text-gray-600">
            已报名 <span class="text-xl font-semibold text-gray-900">{{ countText }}</span> 人
          </div>
        </div>

        <VLoading v-if="loading" />

        <VEmpty v-else-if="registrations.length === 0" title="暂无报名" message="分享活动链接给会员，他们会在这里留下报名记录" />

        <ul v-else class="divide-y divide-gray-100">
          <li v-for="reg in registrations" :key="reg.metadata?.name">
            <VEntity>
              <template #start>
                <VEntityField :title="reg.spec?.name">
                  <template #description>
                    <div class="flex flex-wrap items-center gap-x-4 gap-y-1 py-0.5">
                      <span class="whitespace-nowrap">📱 {{ reg.spec?.phone }}</span>
                      <span v-if="reg.spec?.remark" class="whitespace-nowrap">备注：{{ reg.spec.remark }}</span>
                      <template v-if="reg.spec?.customFields && activity?.spec?.formFields">
                        <span
                          v-for="ff in activity.spec.formFields"
                          :key="ff.name"
                          class="whitespace-nowrap"
                        >
                          {{ ff.label }}：{{ reg.spec.customFields[ff.name] || '-' }}
                        </span>
                      </template>
                      <span class="text-xs text-gray-400">
                        {{ reg.spec?.checkedInAt ? formatTime(reg.spec?.checkedInAt) : formatTime(reg.spec?.registrationTime) }}
                      </span>
                    </div>
                  </template>
                </VEntityField>
              </template>
              <template #end>
                <VEntityField>
                  <template #description>
                    <VStatusDot
                      v-if="reg.spec?.checkedIn"
                      text="✅ 已签到"
                      state="success"
                    />
                    <VStatusDot
                      v-else
                      text="未签到"
                      state="warning"
                    />
                  </template>
                </VEntityField>
                <VEntityField>
                  <template #description>
                    <VStatusDot
                      :text="statusText(reg.spec?.status).text"
                      :state="statusText(reg.spec?.status).type"
                    />
                  </template>
                </VEntityField>
                <VEntityField>
                  <template #description>
                    <VButton
                      v-if="!reg.spec?.checkedIn"
                      size="sm"
                      type="secondary"
                      @click="toggleCheckin(reg)"
                    >
                      签到
                    </VButton>
                    <VButton
                      v-else
                      size="sm"
                      type="default"
                      @click="toggleCheckin(reg)"
                    >
                      取消签到
                    </VButton>
                  </template>
                </VEntityField>
              </template>
            </VEntity>
          </li>
        </ul>

        <div v-if="total > size" class="mt-4 flex justify-end">
          <VPagination :page="page" :size="size" :total="total" @change="onPageChange" />
        </div>
      </VCard>
    </div>
  </div>
</template>
