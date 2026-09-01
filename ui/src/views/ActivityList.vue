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

<style>
.al-page { min-height: 100%; }
.al-container { padding: 16px 24px 40px; max-width: 1080px; }
.al-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.al-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.al-search { flex: 1; max-width: 320px; height: 36px; padding: 0 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 13px; color: #1f2937; outline: none; transition: border-color 0.15s, box-shadow 0.15s; }
.al-search:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }
.al-list { display: flex; flex-direction: column; }
.al-row { display: flex; align-items: center; gap: 16px; padding: 14px 4px; border-bottom: 1px solid #f3f4f6; transition: background 0.15s; }
.al-row:last-child { border-bottom: none; }
.al-row:hover { background: #fafafa; }
.al-main { flex: 1; min-width: 0; }
.al-title { font-size: 14px; font-weight: 600; color: #1f2937; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.al-meta { display: flex; flex-wrap: wrap; gap: 12px; }
.al-meta-item { font-size: 12px; color: #9ca3af; }
.al-tags { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.al-tag { display: inline-flex; align-items: center; padding: 2px 10px; border-radius: 20px; background: #eef4ff; color: #1a4f9e; font-size: 12px; }
.al-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.al-pagination { display: flex; justify-content: flex-end; margin-top: 16px; padding-top: 12px; border-top: 1px solid #f3f4f6; }
@media (max-width: 768px) {
  .al-row { flex-wrap: wrap; gap: 10px; }
  .al-main { flex: 1 1 100%; }
  .al-tags { order: 3; }
  .al-actions { order: 4; width: 100%; justify-content: flex-end; }
}
</style>