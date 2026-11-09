<template>
  <el-dialog v-model="dialogVisible" title="在线购票" width="480" :close-on-click-modal="false" top="12vh">
    <div v-if="spot">
      <!-- 景点摘要 -->
      <div class="spot-brief">
        <el-image :src="spot.cover" fit="cover" style="width: 64px; height: 48px; border-radius: 8px; flex-shrink: 0;">
          <template #error><div class="brief-img-fallback"><i class="fa fa-image"></i></div></template>
        </el-image>
        <div class="brief-info">
          <p class="brief-name">{{ spot.name }}</p>
          <p class="brief-price">{{ priceText(spot.price) }}</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="游玩日期" prop="playDate">
          <el-date-picker
            v-model="form.playDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%;"
            :disabled-date="disabledDate"
          />
          <p class="date-tip">仅可预订今天起 30 天内；当天售票截止时间以景区设置为准</p>
        </el-form-item>
        <el-form-item label="门票数量" prop="ticketCount">
          <el-input-number v-model="form.ticketCount" :min="1" :max="20" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
      </el-form>

      <div class="total-row">
        <span>合计金额</span>
        <span class="total-amount">{{ totalAmount }}</span>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="dialogVisible = false" style="border-radius: 8px;">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit" style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
          确认下单
        </el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 模拟支付弹窗：下单后立即支付；选“稍后支付”则订单保持待支付 -->
  <PayDialog
    v-model="payVisible"
    :name="spot?.name || ''"
    :amount="payAmount"
    :meta="`${form.playDate} · ${form.ticketCount} 张门票`"
    :loading="submitting"
    @pay="doCreate(true)"
    @later="doCreate(false)"
  />
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { priceText } from '@/utils/format'
import { createOrder, payOrder } from '@/api/order'
import { useAuthStore } from '@/stores/auth'
import PayDialog from '@/components/user/PayDialog.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  spot: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'order-created'])

const authStore = useAuthStore()
const formRef = ref(null)
const submitting = ref(false)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const form = reactive({
  playDate: '',
  ticketCount: 1,
  contactName: '',
  contactPhone: ''
})

const rules = {
  playDate: [{ required: true, message: '请选择游玩日期', trigger: 'change' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const totalAmount = computed(() => {
  const p = props.spot?.price || 0
  return p === 0 ? '免费' : `¥${(p * (form.ticketCount || 1)).toLocaleString('zh-CN')}`
})

// 模拟支付弹窗金额
const payVisible = ref(false)
const payAmount = computed(() => ((props.spot?.price || 0) * (form.ticketCount || 1)).toLocaleString('zh-CN'))

function disabledDate(date) {
  const min = new Date()
  min.setHours(0, 0, 0, 0)
  const max = new Date(min)
  max.setDate(max.getDate() + 29)
  return date.getTime() < min.getTime() || date.getTime() > max.getTime()
}

watch(() => props.modelValue, (v) => {
  if (v) {
    form.playDate = ''
    form.ticketCount = 1
    form.contactName = props.spot?.contactName || authStore.userInfo?.nickname || ''
    form.contactPhone = authStore.userInfo?.phone || ''
    formRef.value?.clearValidate()
  }
})

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 付费景点先弹模拟支付窗；免费景点（0 元）直接创建并支付
  if ((props.spot?.price || 0) > 0) {
    payVisible.value = true
  } else {
    doCreate(true)
  }
}

async function doCreate(payNow) {
  submitting.value = true
  try {
    const payload = {
      // 后端关联字段是嵌套对象；金额、支付状态、用户、订单号都由服务端决定
      scenicId: { id: props.spot.id },
      playDate: form.playDate,
      ticketCount: form.ticketCount,
      contactName: form.contactName,
      contactPhone: form.contactPhone
    }
    const res = await createOrder(payload)
    if (res.code !== 1) return

    const created = res.content || {}
    let paid = false
    if (payNow) {
      try {
        const payRes = await payOrder(created.id)
        paid = payRes.code === 1
      } catch (err) {
        // 支付失败不回滚订单，提示用户到「我的订单」继续支付
      }
    }

    dialogVisible.value = false
    payVisible.value = false
    if (paid) {
      ElMessage.success('支付成功，订单已创建')
    } else if ((props.spot?.price || 0) === 0) {
      ElMessage.success('下单成功（免费景点）')
    } else {
      ElMessage.success('下单成功，请在 30 分钟内到「我的订单」完成支付')
    }
    emit('order-created', created)
  } catch (err) {
    dialogVisible.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.spot-brief {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #F8FAFC;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 18px;
}

.brief-img-fallback {
  width: 64px; height: 48px;
  background: #E2E8F0;
  display: flex; align-items: center; justify-content: center;
  color: #94A3B8;
  border-radius: 8px;
}

.brief-info { min-width: 0; }

.date-tip {
  width: 100%;
  font-size: 12px;
  color: #94A3B8;
  margin: 4px 0 0;
  line-height: 1.5;
}

.brief-name {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brief-price {
  font-size: 14px;
  font-weight: 700;
  color: #059669;
  margin: 0;
}

.total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #FFF8ED;
  border: 1px solid #FDE68A;
  border-radius: 10px;
  padding: 12px 16px;
  font-size: 13px;
  color: #92400E;
}

.total-amount {
  font-size: 18px;
  font-weight: 700;
  color: #D97706;
}
</style>
