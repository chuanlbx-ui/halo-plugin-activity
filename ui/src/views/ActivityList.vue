<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
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
  VTag,
  Toast,
  IconAddCircle,
  IconDeleteBin,
  IconList,
} from '@halo-dev/components'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const activities = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const deleting = ref(false)

interface ListResult {
  items: any[]
  total: number
  page: number
  size: number
}

const API_BASE = '/apis/console.api.activity.halo.run/v1alpha1'

async function fetchActivities() {
  loading.value = true
  try {
    const { data } = await axios.get<ListResult>(`${API_BASE}/activities`, {
      params: {
        page: page.value,
        size: size.value,
        keyword: keyword.value || undefined,
      },
    })
    activities.value = data.items || []
    total.value = data.total || 0
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '加载活动列表失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  fetchActivities()
}

function onPageChange(value: { page: number; size: number }) {
  page.value = value.page
  fetchActivities()
}

async function onDelete(activity: any) {
  const name = activity.metadata?.name
  if (!name) return
  deleting.value = true
  try {
    await axios.delete(`${API_BASE}/activities/${name}`)
    Toast.success('删除成功')
    fetchActivities()
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '删除失败')
  } finally {
    deleting.value = false
  }
}

function goCreate() {
  router.push({ name: 'ActivityCreate' })
}

function goEdit(activity: any) {
  router.push({ name: 'ActivityEdit', params: { name: activity.metadata?.name } })
}

function goRegistrations(activity: any) {
  router.push({
    name: 'ActivityRegistrations',
    params: { name: activity.metadata?.name },
  })
}

function formatTime(iso?: string) {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

function statusText(status?: string) {
  const map: Record<string, { text: string; type: 'success' | 'warning' | 'error' | 'default' }> = {
    PUBLISHED: { text: '已发布', type: 'success' },
    DRAFT: { text: '草稿', type: 'default' },
    ENDED: { text: '已结束', type: 'warning' },
    FULL: { text: '已满员', type: 'error' },
  }
  return map[status || 'DRAFT'] || { text: status || '未知', type: 'default' }
}

onMounted(fetchActivities)
</script>

<template>
  <div>
    <VPageHeader title="活动管理">
      <template #actions>
        <VButton type="primary" @click="goCreate">
          <template #icon>
            <IconAddCircle />
          </template>
          新建活动
        </VButton>
      </template>
    </VPageHeader>

    <div class="p-4">
      <VCard :body-class="['p-4']">
        <div class="mb-4 flex items-center gap-3">
          <input
            v-model="keyword"
            class="h-9 flex-1 rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary"
            placeholder="搜索活动标题…"
            @keyup.enter="onSearch"
          />
          <VButton @click="onSearch">搜索</VButton>
        </div>

        <VLoading v-if="loading" />

        <VEmpty v-else-if="activities.length === 0" title="暂无活动" message="点击右上角「新建活动」创建第一个活动" />

        <ul v-else class="divide-y divide-gray-100">
          <li v-for="activity in activities" :key="activity.metadata?.name">
            <VEntity>
              <template #start>
                <VEntityField :title="activity.spec?.title">
                  <template #description>
                    <span v-if="activity.spec?.location" class="mr-2">📍 {{ activity.spec.location }}</span>
                    <span v-if="activity.spec?.startTime">{{ formatTime(activity.spec.startTime) }}</span>
                  </template>
                </VEntityField>
              </template>
              <template #end>
                <VEntityField>
                  <template #description>
                    <VStatusDot
                      :text="statusText(activity.spec?.status).text"
                      :state="statusText(activity.spec?.status).type"
                    />
                  </template>
                </VEntityField>
                <VEntityField>
                  <template #description>
                    <VTag v-if="activity.spec?.quota && activity.spec.quota > 0">
                      名额 {{ activity.spec.quota }}
                    </VTag>
                    <VTag v-else>不限名额</VTag>
                  </template>
                </VEntityField>
                <VEntityField>
                  <template #description>
                    <div class="flex items-center gap-2">
                      <VButton size="sm" @click="goRegistrations(activity)">
                        <template #icon>
                          <IconList />
                        </template>
                        报名记录
                      </VButton>
                      <VButton size="sm" type="secondary" @click="goEdit(activity)">
                        编辑
                      </VButton>
                      <VButton size="sm" type="danger" :loading="deleting" @click="onDelete(activity)">
                        <template #icon>
                          <IconDeleteBin />
                        </template>
                        删除
                      </VButton>
                    </div>
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

<style scoped>
input:focus {
  border-color: #4b5563;
}
</style>
