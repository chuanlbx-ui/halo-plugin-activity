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
  <div class="al-page">
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

    <div class="al-container">
      <div class="al-card">
        <div class="al-toolbar">
          <input v-model="keyword" class="al-search" placeholder="搜索活动标题…" @keyup.enter="onSearch" />
          <VButton @click="onSearch">搜索</VButton>
        </div>

        <VLoading v-if="loading" />

        <VEmpty v-else-if="activities.length === 0" title="暂无活动" message="点击右上角「新建活动」创建第一个活动" />

        <div v-else class="al-list">
          <div v-for="activity in activities" :key="activity.metadata?.name" class="al-row">
            <div class="al-main">
              <div class="al-title">{{ activity.spec?.title }}</div>
              <div class="al-meta">
                <span v-if="activity.spec?.location" class="al-meta-item">📍 {{ activity.spec.location }}</span>
                <span v-if="activity.spec?.startTime" class="al-meta-item">{{ formatTime(activity.spec.startTime) }}</span>
              </div>
            </div>

            <div class="al-tags">
              <VStatusDot :text="statusText(activity.spec?.status).text" :state="statusText(activity.spec?.status).type" />
              <span class="al-tag">{{ activity.spec?.quota && activity.spec.quota > 0 ? '名额 ' + activity.spec.quota : '不限名额' }}</span>
            </div>

            <div class="al-actions">
              <VButton size="sm" @click="goRegistrations(activity)">
                <template #icon>
                  <IconList />
                </template>
                报名记录
              </VButton>
              <VButton size="sm" type="secondary" @click="goEdit(activity)">编辑</VButton>
              <VButton size="sm" type="danger" :loading="deleting" @click="onDelete(activity)">
                <template #icon>
                  <IconDeleteBin />
                </template>
                删除
              </VButton>
            </div>
          </div>
        </div>

        <div v-if="total > size" class="al-pagination">
          <VPagination :page="page" :size="size" :total="total" @change="onPageChange" />
        </div>
      </div>
    </div>
  </div>
</template>
