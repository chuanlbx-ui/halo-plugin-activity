<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  VButton,
  VCard,
  VPageHeader,
  VLoading,
  Toast,
} from '@halo-dev/components'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const isEdit = ref(false)

const form = ref({
  title: '',
  cover: '',
  location: '',
  startTime: '',
  endTime: '',
  registrationDeadline: '',
  quota: 0,
  status: 'PUBLISHED',
  content: '',
})

const API_BASE = '/apis/console.api.activity.halo.run/v1alpha1'

function toLocalInput(iso?: string) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function toISO(local: string) {
  if (!local) return undefined
  const d = new Date(local)
  return d.toISOString()
}

async function loadActivity(name: string) {
  loading.value = true
  try {
    const { data } = await axios.get(`${API_BASE}/activities/${name}`)
    const spec = data.spec || {}
    form.value = {
      title: spec.title || '',
      cover: spec.cover || '',
      location: spec.location || '',
      startTime: toLocalInput(spec.startTime),
      endTime: toLocalInput(spec.endTime),
      registrationDeadline: toLocalInput(spec.registrationDeadline),
      quota: spec.quota || 0,
      status: spec.status || 'PUBLISHED',
      content: spec.content || '',
    }
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '加载活动失败')
    router.back()
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!form.value.title.trim()) {
    Toast.error('请填写活动标题')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      apiVersion: 'activity.halo.run/v1alpha1',
      kind: 'Activity',
      metadata: {},
      spec: {
        title: form.value.title.trim(),
        cover: form.value.cover || undefined,
        location: form.value.location || undefined,
        startTime: toISO(form.value.startTime),
        endTime: toISO(form.value.endTime),
        registrationDeadline: toISO(form.value.registrationDeadline),
        quota: Number(form.value.quota) || 0,
        status: form.value.status,
        content: form.value.content || undefined,
      },
    }
    if (isEdit.value) {
      const name = String(route.params.name)
      payload.metadata.name = name
      await axios.put(`${API_BASE}/activities/${name}`, payload)
      Toast.success('保存成功')
    } else {
      await axios.post(`${API_BASE}/activities`, payload)
      Toast.success('创建成功')
    }
    router.push({ name: 'Activities' })
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  const name = route.params.name
  if (name) {
    isEdit.value = true
    loadActivity(String(name))
  }
})
</script>

<template>
  <div>
    <VPageHeader :title="isEdit ? '编辑活动' : '新建活动'">
      <template #actions>
        <VButton type="secondary" @click="router.back()">返回</VButton>
        <VButton type="primary" :loading="saving" @click="onSubmit">
          {{ isEdit ? '保存修改' : '创建活动' }}
        </VButton>
      </template>
    </VPageHeader>

    <div class="p-4">
      <VLoading v-if="loading" />
      <VCard v-else :body-class="['p-6']">
        <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
          <div class="md:col-span-2">
            <label class="mb-1 block text-sm font-medium text-gray-700">活动标题 *</label>
            <input
              v-model="form.title"
              class="h-10 w-full rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary"
              placeholder="例如：AI 数字工匠训练营（第三期）"
            />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">封面图 URL</label>
            <input
              v-model="form.cover"
              class="h-10 w-full rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary"
              placeholder="https://…"
            />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">活动地点</label>
            <input
              v-model="form.location"
              class="h-10 w-full rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary"
              placeholder="例如：文山州互联网协会会议室"
            />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">开始时间</label>
            <input v-model="form.startTime" type="datetime-local"
              class="h-10 w-full rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary" />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">结束时间</label>
            <input v-model="form.endTime" type="datetime-local"
              class="h-10 w-full rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary" />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">报名截止时间</label>
            <input v-model="form.registrationDeadline" type="datetime-local"
              class="h-10 w-full rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary" />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">报名名额（0 = 不限）</label>
            <input v-model.number="form.quota" type="number" min="0"
              class="h-10 w-full rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary" />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">状态</label>
            <select v-model="form.status"
              class="h-10 w-full rounded border border-gray-300 px-3 text-sm outline-none focus:border-primary">
              <option value="PUBLISHED">已发布</option>
              <option value="DRAFT">草稿</option>
              <option value="ENDED">已结束</option>
            </select>
          </div>

          <div class="md:col-span-2">
            <label class="mb-1 block text-sm font-medium text-gray-700">活动详情（支持 HTML）</label>
            <textarea
              v-model="form.content"
              rows="8"
              class="w-full rounded border border-gray-300 px-3 py-2 text-sm outline-none focus:border-primary"
              placeholder="活动介绍、议程、注意事项…"
            ></textarea>
          </div>
        </div>
      </VCard>
    </div>
  </div>
</template>

<style scoped>
input:focus,
select:focus,
textarea:focus {
  border-color: #4b5563;
}
</style>
