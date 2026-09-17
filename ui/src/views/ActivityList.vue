<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  VButton,
  VEmpty,
  VLoading,
  VPageHeader,
  VPagination,
  VStatusDot,
  Toast,
  IconAddCircle,
  IconDeleteBin,
  IconList,
} from '@halo-dev/components'
import axios from 'axios'
import * as XLSX from 'xlsx'
import QRCode from 'qrcode'

const router = useRouter()
const loading = ref(false)
const activities = ref<any[]>([])
const registrations = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const deleting = ref(false)

// 二维码弹窗
const qrVisible = ref(false)
const qrDataUrl = ref('')
const qrActivity = ref<any>(null)
const qrLoading = ref(false)

const API_BASE = '/apis/console.api.activity.halo.run/v1alpha1'

async function fetchActivities() {
  loading.value = true
  try {
    const { data } = await axios.get(`${API_BASE}/activities`, {
      params: { page: page.value, size: size.value, keyword: keyword.value || undefined },
    })
    activities.value = data.items || []
    total.value = data.total || 0
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '加载活动列表失败')
  } finally {
    loading.value = false
  }
  // 报名数据单独拉（用于统计与导出），失败不影响列表
  try {
    const { data } = await axios.get(`${API_BASE}/registrations`, { params: { page: 1, size: 5000 } })
    registrations.value = data.items || []
  } catch {
    registrations.value = []
  }
}

/** 每个活动的报名/签到统计 */
const statsMap = computed(() => {
  const map: Record<string, { reg: number; checked: number }> = {}
  for (const r of registrations.value) {
    const name = r.spec?.activityName
    if (!name) continue
    if (!map[name]) map[name] = { reg: 0, checked: 0 }
    map[name].reg += 1
    if (r.spec?.checkedIn) map[name].checked += 1
  }
  return map
})

function statsOf(activity: any) {
  const s = statsMap.value[activity.metadata?.name] || { reg: 0, checked: 0 }
  return { ...s, rate: s.reg > 0 ? Math.round((s.checked / s.reg) * 100) : 0 }
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
  router.push({ name: 'ActivityRegistrations', params: { name: activity.metadata?.name } })
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

/** 导出该活动报名名单为 Excel */
function exportExcel(activity: any) {
  const name = activity.metadata?.name
  const activityTitle = activity.spec?.title || '活动'
  const list = registrations.value.filter((r) => r.spec?.activityName === name)
  if (list.length === 0) {
    Toast.warning('该活动暂无报名记录')
    return
  }
  // 动态列：活动自定义字段
  const customFields: any[] = Array.isArray(activity.spec?.formFields) ? activity.spec.formFields : []
  const rows = list.map((r: any, i: number) => {
    const s = r.spec || {}
    const row: Record<string, any> = {
      序号: i + 1,
      姓名: s.name || '',
      手机号: s.phone || '',
      报名时间: s.registrationTime ? new Date(s.registrationTime).toLocaleString('zh-CN', { hour12: false }) : '',
      是否签到: s.checkedIn ? '已签到' : '未签到',
      签到时间: s.checkedInAt ? new Date(s.checkedInAt).toLocaleString('zh-CN', { hour12: false }) : '',
      备注: s.remark || '',
    }
    for (const f of customFields) {
      if (f?.label) row[f.label] = s.customFields?.[f.name] || ''
    }
    return row
  })
  const ws = XLSX.utils.json_to_sheet(rows)
  ws['!cols'] = Object.keys(rows[0] || {}).map((k) => ({ wch: k === '备注' ? 24 : k === '手机号' ? 14 : 18 }))
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '报名名单')
  const st = statsOf(activity)
  XLSX.utils.book_append_sheet(
    wb,
    XLSX.utils.aoa_to_sheet([
      ['活动名称', activityTitle],
      ['活动时间', formatTime(activity.spec?.startTime)],
      ['活动地点', activity.spec?.location || ''],
      ['活动状态', statusText(activity.spec?.status).text],
      ['名额', activity.spec?.quota ?? ''],
      ['报名人数', st.reg],
      ['已签到', st.checked],
      ['签到率', st.reg ? st.rate + '%' : '-'],
      ['导出时间', new Date().toLocaleString('zh-CN', { hour12: false })],
    ]),
    '活动概况'
  )
  XLSX.writeFile(wb, `${String(activityTitle).replace(/[\\/:*?"<>|]/g, '_').slice(0, 40)}_报名名单.xlsx`)
  Toast.success(`已导出 ${list.length} 条报名记录`)
}

/** 生成签到二维码（指向公开签到页） */
async function showQr(activity: any) {
  qrActivity.value = activity
  qrVisible.value = true
  qrDataUrl.value = ''
  qrLoading.value = true
  const url = `https://wenbita.cn/checkin.html?activity=${activity.metadata?.name}`
  try {
    qrDataUrl.value = await QRCode.toDataURL(url, {
      width: 640,
      margin: 2,
      color: { dark: '#0a2a5e', light: '#ffffff' },
      errorCorrectionLevel: 'M',
    })
  } catch {
    qrDataUrl.value = ''
  }
  qrLoading.value = false
}

const qrUrl = computed(() =>
  qrActivity.value ? `https://wenbita.cn/checkin.html?activity=${qrActivity.value.metadata?.name}` : ''
)

async function copyQrUrl() {
  try {
    await navigator.clipboard.writeText(qrUrl.value)
    Toast.success('已复制签到链接')
  } catch {
    window.prompt('复制签到链接：', qrUrl.value)
  }
}

function downloadQr() {
  if (!qrDataUrl.value) return
  const a = document.createElement('a')
  a.href = qrDataUrl.value
  a.download = `签到二维码_${qrActivity.value?.spec?.title || '活动'}.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
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
              <div class="al-note" v-if="(activity.spec?.coverageLinks || []).length">
                📰 已配 {{ activity.spec.coverageLinks.length }} 条活动报道
              </div>
            </div>

            <div class="al-stats">
              <div class="al-stat">
                <span class="al-stat-num">{{ statsOf(activity).reg }}</span>
                <span class="al-stat-label">报名</span>
              </div>
              <div class="al-stat">
                <span class="al-stat-num al-stat-checked">{{ statsOf(activity).checked }}</span>
                <span class="al-stat-label">签到</span>
              </div>
              <div class="al-stat">
                <span class="al-stat-num">{{ statsOf(activity).rate }}%</span>
                <span class="al-stat-label">签到率</span>
              </div>
              <div class="al-progress">
                <div
                  class="al-progress-bar"
                  :style="{
                    width:
                      (activity.spec?.quota > 0
                        ? Math.min(Math.round((statsOf(activity).reg / activity.spec.quota) * 100), 100)
                        : 0) + '%',
                  }"
                ></div>
              </div>
              <div class="al-quota">
                {{ activity.spec?.quota > 0 ? `${statsOf(activity).reg}/${activity.spec.quota} 名额` : '不限名额' }}
              </div>
            </div>

            <div class="al-tags">
              <VStatusDot :text="statusText(activity.spec?.status).text" :state="statusText(activity.spec?.status).type" />
            </div>

            <div class="al-actions">
              <VButton size="sm" @click="goRegistrations(activity)">
                <template #icon>
                  <IconList />
                </template>
                报名记录
              </VButton>
              <VButton size="sm" @click="exportExcel(activity)">📊 导出 Excel</VButton>
              <VButton size="sm" @click="showQr(activity)">📱 签到二维码</VButton>
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

    <!-- 签到二维码弹窗 -->
    <Teleport to="body">
      <div v-if="qrVisible" class="al-qr-mask" @click.self="qrVisible = false">
        <div class="al-qr-box">
          <div class="al-qr-head">
            <strong>📱 签到二维码</strong>
            <span class="al-qr-close" @click="qrVisible = false">×</span>
          </div>
          <div class="al-qr-title">{{ qrActivity?.spec?.title }}</div>
          <div class="al-qr-tip">现场大屏展示或打印张贴，参与者扫码进入签到页</div>
          <div class="al-qr-img">
            <span v-if="qrLoading">生成中…</span>
            <img v-else-if="qrDataUrl" :src="qrDataUrl" alt="签到二维码" />
            <span v-else style="color: #c00">生成失败，请重试</span>
          </div>
          <div class="al-qr-url">{{ qrUrl }}</div>
          <div class="al-qr-actions">
            <VButton size="sm" @click="copyQrUrl">🔗 复制链接</VButton>
            <VButton size="sm" type="primary" @click="downloadQr">⬇️ 下载二维码</VButton>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
