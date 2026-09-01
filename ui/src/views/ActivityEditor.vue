<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  VButton,
  VCard,
  VPageHeader,
  VLoading,
  Toast,
} from '@halo-dev/components'
import { RichTextEditor, VueEditor, ExtensionsKit } from '@halo-dev/richtext-editor'
import { consoleApiClient, type Attachment } from '@halo-dev/api-client'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const coverUploading = ref(false)

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
  metadataVersion: '',
})

const API_BASE = '/apis/console.api.activity.halo.run/v1alpha1'

// ---------- 富文本编辑器 ----------
const editor = new VueEditor({
  extensions: [
    ExtensionsKit.configure({
      upload: {
        upload: (fileOrUrl: File | string) => uploadAttachment(fileOrUrl),
      },
    }),
  ],
  content: '',
  onUpdate: ({ editor: ed }) => {
    form.value.content = ed.getHTML()
  },
})

async function uploadAttachment(fileOrUrl: File | string): Promise<Attachment | undefined> {
  try {
    const { data } = await consoleApiClient.storage.attachment.uploadAttachmentForConsole(
      fileOrUrl instanceof File ? { file: fileOrUrl } : { url: fileOrUrl }
    )
    return data
  } catch (e: any) {
    Toast.error(e?.response?.data?.message || '图片上传失败')
    return undefined
  }
}

function onPickCover(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  coverUploading.value = true
  uploadAttachment(file)
    .then((att) => {
      if (att) {
        form.value.cover = att.status?.permalink || ''
      }
    })
    .finally(() => {
      coverUploading.value = false
      input.value = ''
    })
}

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
      metadataVersion: data.metadata?.version || '',
    }
    if (editor.commands) {
      editor.commands.setContent(spec.content || '')
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
      metadata: {
        version: form.value.metadataVersion || undefined,
      },
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

onUnmounted(() => {
  // 组件卸载时销毁编辑器，避免 TipTap keyed plugin 重复注册导致 RangeError
  try {
    editor.destroy?.()
  } catch (e) {
    // ignore
  }
})
</script>

<template>
  <div class="ae-page">
    <VPageHeader :title="isEdit ? '编辑活动' : '新建活动'">
      <template #actions>
        <VButton type="secondary" @click="router.back()">返回</VButton>
        <VButton type="primary" :loading="saving" @click="onSubmit">
          {{ isEdit ? '保存修改' : '创建活动' }}
        </VButton>
      </template>
    </VPageHeader>

    <div class="ae-container">
      <VLoading v-if="loading" />

      <div v-else class="ae-form">
        <section class="ae-section">
          <div class="ae-section-title">
            <span class="ae-section-icon">📋</span>
            <div>
              <h3>基本信息</h3>
              <p>活动标题与封面图，标题会显示在活动列表和详情页</p>
            </div>
          </div>

          <div class="ae-field ae-field-full">
            <label class="ae-label">活动标题 <span class="ae-req">*</span></label>
            <input v-model="form.title" class="ae-input" placeholder="例如：AI 数字工匠训练营（第三期）" />
          </div>

          <div class="ae-field ae-field-full">
            <label class="ae-label">活动封面图</label>
            <div class="ae-cover-row">
              <div class="ae-cover-controls">
                <div class="ae-cover-btns">
                  <label class="ae-upload-btn">
                    {{ coverUploading ? '上传中…' : '📁 上传图片' }}
                    <input type="file" accept="image/*" class="hidden" :disabled="coverUploading" @change="onPickCover" />
                  </label>
                  <span class="ae-or">或填写图片 URL</span>
                </div>
                <input v-model="form.cover" class="ae-input" placeholder="https://…" />
              </div>
              <div v-if="form.cover" class="ae-cover-preview">
                <img :src="form.cover" alt="封面预览" />
              </div>
            </div>
          </div>
        </section>

        <section class="ae-section">
          <div class="ae-section-title">
            <span class="ae-section-icon">🕐</span>
            <div>
              <h3>时间与地点</h3>
              <p>活动举办的时间地点，报名截止默认在开始前</p>
            </div>
          </div>

          <div class="ae-grid-2">
            <div class="ae-field">
              <label class="ae-label">活动地点</label>
              <input v-model="form.location" class="ae-input" placeholder="例如：文山州互联网协会会议室" />
            </div>
            <div class="ae-field">
              <label class="ae-label">报名名额 <span class="ae-hint">（0 = 不限）</span></label>
              <input v-model.number="form.quota" type="number" min="0" class="ae-input" />
            </div>
            <div class="ae-field">
              <label class="ae-label">开始时间</label>
              <input v-model="form.startTime" type="datetime-local" class="ae-input" />
            </div>
            <div class="ae-field">
              <label class="ae-label">结束时间</label>
              <input v-model="form.endTime" type="datetime-local" class="ae-input" />
            </div>
            <div class="ae-field">
              <label class="ae-label">报名截止时间</label>
              <input v-model="form.registrationDeadline" type="datetime-local" class="ae-input" />
            </div>
            <div class="ae-field">
              <label class="ae-label">状态</label>
              <select v-model="form.status" class="ae-input">
                <option value="PUBLISHED">已发布（前台可见可报名）</option>
                <option value="DRAFT">草稿（前台不可见）</option>
                <option value="ENDED">已结束</option>
              </select>
            </div>
          </div>
        </section>

        <section class="ae-section">
          <div class="ae-section-title">
            <span class="ae-section-icon">📝</span>
            <div>
              <h3>活动详情</h3>
              <p>详细介绍活动内容、议程、适合人群，支持富文本格式</p>
            </div>
          </div>

          <div class="ae-field ae-field-full">
            <div class="richtext-wrap">
              <RichTextEditor :editor="editor" locale="zh-CN" />
            </div>
          </div>
        </section>

        <section class="ae-section">
          <div class="ae-section-title">
            <span class="ae-section-icon">🧩</span>
            <div class="ae-section-title-text">
              <h3>报名表单自定义字段</h3>
              <p>姓名、手机号为内置必填，以下字段可自由增删配置</p>
            </div>
            <VButton size="sm" type="secondary" class="ae-add-btn" @click="addField">+ 添加字段</VButton>
          </div>

          <div v-if="form.formFields.length === 0" class="ae-empty-fields">
            暂无自定义字段。点「添加字段」可增加如公司名称、职务、参加人数等报名必填/选填项。
          </div>

          <div v-for="(field, idx) in form.formFields" :key="idx" class="ae-field-card">
            <div class="ae-field-card-head">
              <span class="ae-field-card-no">字段 {{ idx + 1 }}</span>
              <div class="ae-field-card-ops">
                <button type="button" class="ae-icon-btn" @click="moveField(idx, -1)" :disabled="idx === 0" title="上移">↑</button>
                <button type="button" class="ae-icon-btn" @click="moveField(idx, 1)" :disabled="idx === form.formFields.length - 1" title="下移">↓</button>
                <button type="button" class="ae-icon-btn ae-icon-btn-danger" @click="removeField(idx)" title="删除">✕ 删除</button>
              </div>
            </div>
            <div class="ae-grid-4">
              <div class="ae-field">
                <label class="ae-label ae-label-sm">字段名（英文）<span class="ae-req">*</span></label>
                <input v-model="field.name" class="ae-input" placeholder="company"
                  @blur="field.name = field.name.trim(); if (!field.label && field.name) field.label = nameToLabel(field.name)" />
              </div>
              <div class="ae-field">
                <label class="ae-label ae-label-sm">显示名称 <span class="ae-req">*</span></label>
                <input v-model="field.label" class="ae-input" placeholder="公司名称" />
              </div>
              <div class="ae-field">
                <label class="ae-label ae-label-sm">类型</label>
                <select v-model="field.type" class="ae-input">
                  <option value="text">单行文本</option>
                  <option value="textarea">多行文本</option>
                  <option value="select">下拉选择</option>
                  <option value="number">数字</option>
                </select>
              </div>
              <div class="ae-field">
                <label class="ae-label ae-label-sm">是否必填</label>
                <select v-model="field.required" class="ae-input">
                  <option :value="false">选填</option>
                  <option :value="true">必填</option>
                </select>
              </div>
              <div v-if="field.type === 'select'" class="ae-field ae-span-2">
                <label class="ae-label ae-label-sm">选项（逗号分隔）<span class="ae-req">*</span></label>
                <input v-model="field.options" class="ae-input" placeholder="选项一,选项二,选项三" />
              </div>
              <div class="ae-field ae-span-2">
                <label class="ae-label ae-label-sm">占位提示（选填）</label>
                <input v-model="field.placeholder" class="ae-input" placeholder="请输入…" />
              </div>
            </div>
          </div>
        </section>

        <div class="ae-footer">
          <VButton type="secondary" @click="router.back()">取消</VButton>
          <VButton type="primary" :loading="saving" @click="onSubmit">
            {{ isEdit ? '保存修改' : '创建活动' }}
          </VButton>
        </div>
      </div>
    </div>
  </div>
</template>
