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
  formFields: [] as any[],
})

const API_BASE = '/apis/console.api.activity.halo.run/v1alpha1'

function emptyField() {
  return {
    name: '',
    label: '',
    type: 'text',
    required: false,
    options: '',
    placeholder: '',
  }
}

function addField() {
  form.value.formFields.push(emptyField())
}

function removeField(index: number) {
  form.value.formFields.splice(index, 1)
}

function moveField(index: number, dir: -1 | 1) {
  const target = index + dir
  if (target < 0 || target >= form.value.formFields.length) return
  const arr = form.value.formFields
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
}

function nameToLabel(name: string) {
  return name
    .replace(/([A-Z])/g, '_$1')
    .toLowerCase()
    .replace(/^_/, '')
    .replace(/[_\s]+/g, ' ')
    .replace(/^\w/, c => c.toUpperCase())
}

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
      formFields: Array.isArray(spec.formFields) ? spec.formFields.map((f: any) => ({ ...f })) : [],
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
  // 校验自定义字段配置
  const fields = form.value.formFields.filter((f: any) => f.name || f.label)
  for (const f of fields) {
    if (!f.name.trim()) {
      Toast.error('自定义字段「字段名」不能为空')
      return
    }
    if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(f.name.trim())) {
      Toast.error(`字段名「${f.name}」只能包含英文字母、数字和下划线，且以字母开头`)
      return
    }
    if (!f.label.trim()) {
      Toast.error(`字段「${f.name}」的显示名称不能为空`)
      return
    }
    if (f.type === 'select' && !f.options.trim()) {
      Toast.error(`字段「${f.label}」为下拉选择，请填写选项（逗号分隔）`)
      return
    }
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
        formFields: fields.map((f: any) => ({
          name: f.name.trim(),
          label: f.label.trim(),
          type: f.type || 'text',
          required: !!f.required,
          options: f.options?.trim() || undefined,
          placeholder: f.placeholder?.trim() || undefined,
        })),
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

          <div class="md:col-span-2">
            <div class="mb-2 flex items-center justify-between">
              <label class="block text-sm font-medium text-gray-700">
                报名表单自定义字段
                <span class="ml-1 text-xs font-normal text-gray-400">（姓名、手机号为内置必填，以下字段可自由增删配置）</span>
              </label>
              <VButton size="sm" type="secondary" @click="addField">+ 添加字段</VButton>
            </div>

            <div v-if="form.formFields.length === 0" class="rounded border border-dashed border-gray-300 p-4 text-center text-sm text-gray-400">
              暂无自定义字段。点「添加字段」可增加如公司名称、职务、参加人数等报名必填/选填项。
            </div>

            <div v-for="(field, idx) in form.formFields" :key="idx" class="mb-3 rounded border border-gray-200 p-3">
              <div class="mb-2 flex items-center justify-between">
                <span class="text-xs font-semibold text-gray-500">字段 {{ idx + 1 }}</span>
                <div class="flex items-center gap-1">
                  <button type="button" class="rounded px-1.5 py-0.5 text-gray-400 hover:bg-gray-100" @click="moveField(idx, -1)" :disabled="idx === 0">↑</button>
                  <button type="button" class="rounded px-1.5 py-0.5 text-gray-400 hover:bg-gray-100" @click="moveField(idx, 1)" :disabled="idx === form.formFields.length - 1">↓</button>
                  <button type="button" class="rounded px-1.5 py-0.5 text-red-400 hover:bg-red-50" @click="removeField(idx)">✕ 删除</button>
                </div>
              </div>
              <div class="grid grid-cols-2 gap-2 md:grid-cols-4">
                <div>
                  <label class="mb-0.5 block text-xs text-gray-500">字段名（英文）*</label>
                  <input v-model="field.name" class="h-8 w-full rounded border border-gray-300 px-2 text-xs outline-none focus:border-primary" placeholder="company" @blur="field.name = field.name.trim(); if (!field.label && field.name) field.label = nameToLabel(field.name)" />
                </div>
                <div>
                  <label class="mb-0.5 block text-xs text-gray-500">显示名称 *</label>
                  <input v-model="field.label" class="h-8 w-full rounded border border-gray-300 px-2 text-xs outline-none focus:border-primary" placeholder="公司名称" />
                </div>
                <div>
                  <label class="mb-0.5 block text-xs text-gray-500">类型</label>
                  <select v-model="field.type" class="h-8 w-full rounded border border-gray-300 px-1 text-xs outline-none focus:border-primary">
                    <option value="text">单行文本</option>
                    <option value="textarea">多行文本</option>
                    <option value="select">下拉选择</option>
                    <option value="number">数字</option>
                  </select>
                </div>
                <div>
                  <label class="mb-0.5 block text-xs text-gray-500">是否必填</label>
                  <select v-model="field.required" class="h-8 w-full rounded border border-gray-300 px-1 text-xs outline-none focus:border-primary">
                    <option :value="false">选填</option>
                    <option :value="true">必填</option>
                  </select>
                </div>
                <div v-if="field.type === 'select'" class="col-span-2">
                  <label class="mb-0.5 block text-xs text-gray-500">选项（逗号分隔）*</label>
                  <input v-model="field.options" class="h-8 w-full rounded border border-gray-300 px-2 text-xs outline-none focus:border-primary" placeholder="选项一,选项二,选项三" />
                </div>
                <div class="col-span-2">
                  <label class="mb-0.5 block text-xs text-gray-500">占位提示（选填）</label>
                  <input v-model="field.placeholder" class="h-8 w-full rounded border border-gray-300 px-2 text-xs outline-none focus:border-primary" placeholder="请输入…" />
                </div>
              </div>
            </div>
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
