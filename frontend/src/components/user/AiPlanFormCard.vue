<template>
  <div class="plan-form">
    <div class="plan-form-head">
      <i class="fa fa-magic"></i>
      <span>填一下，我来排行程</span>
    </div>

    <label class="field">
      <span class="label">目的地 <b>*</b></span>
      <el-input v-model="form.destination" size="small" placeholder="如：北京、西安" maxlength="30" @keyup.enter="submit" />
    </label>

    <div class="field-row">
      <label class="field">
        <span class="label">天数</span>
        <el-select v-model="form.days" size="small">
          <el-option v-for="d in 7" :key="d" :label="`${d} 天`" :value="d" />
        </el-select>
      </label>
      <label class="field">
        <span class="label">预算（元）</span>
        <el-input-number v-model="form.budget" size="small" :min="0" :step="500" controls-position="right" />
      </label>
    </div>

    <label class="field">
      <span class="label">偏好</span>
      <el-select v-model="form.preference" size="small" clearable placeholder="可不选">
        <el-option v-for="p in preferences" :key="p" :label="p" :value="p" />
      </el-select>
    </label>

    <label class="field">
      <span class="label">补充</span>
      <el-input v-model="form.extra" size="small" placeholder="如：带老人，不想太累" maxlength="200" />
    </label>

    <div class="plan-form-actions">
      <el-button size="small" @click="$emit('cancel')">取消</el-button>
      <el-button size="small" type="primary" @click="submit">生成行程</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

const props = defineProps({
  /** 重新规划时带上一次的条件，省得用户重填 */
  initial: { type: Object, default: null }
})

const emit = defineEmits(['submit', 'cancel'])

const preferences = ['历史人文', '自然风光', '亲子', '美食', '拍照打卡', '轻松休闲']

const form = reactive({
  destination: props.initial?.destination || '',
  days: props.initial?.days || 2,
  budget: props.initial?.budget ?? 1500,
  preference: props.initial?.preference || '',
  extra: props.initial?.extra || ''
})

function submit() {
  if (!form.destination.trim()) {
    return
  }
  // 交给父组件去调接口，卡片本身不关心网络
  emit('submit', { ...form, destination: form.destination.trim() })
}
</script>

<style scoped>
.plan-form {
  background: #fff;
  border: 1px solid #e8eef5;
  border-radius: 14px;
  padding: 12px 14px;
  margin: 6px 0;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}

.plan-form-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 10px;
}

.plan-form-head i {
  color: #1cb89d;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.label {
  font-size: 11px;
  color: #6b7280;
}

.label b {
  color: #e24b4a;
}

.field :deep(.el-select),
.field :deep(.el-input-number) {
  width: 100%;
}

.plan-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}
</style>
