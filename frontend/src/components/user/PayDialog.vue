<template>
  <el-dialog
    :model-value="modelValue"
    title="模拟支付"
    width="380"
    :close-on-click-modal="false"
    append-to-body
    top="22vh"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <div class="pay-body">
      <p class="pay-name">{{ name }}</p>
      <p class="pay-amount">¥{{ amount }}</p>
      <p v-if="meta" class="pay-meta">{{ meta }}</p>
      <p class="pay-tip"><i class="fa fa-info-circle"></i> 这是模拟支付，不会产生真实扣款</p>
    </div>
    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button :disabled="loading" @click="emit('later')" style="border-radius: 8px;">稍后支付</el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="emit('pay')"
          style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;"
        >
          <i class="fa fa-check mr-1"></i>确认支付
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
// 模拟支付弹窗（购票下单 / 我的订单补支付共用）
// pay：确认支付；later：稍后支付；点 X 关闭则不触发任何操作
defineProps({
  modelValue: { type: Boolean, default: false },
  name: { type: String, default: '' },
  amount: { type: [String, Number], default: '0' },
  meta: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'pay', 'later'])
</script>

<style scoped>
.pay-body {
  text-align: center;
  padding: 8px 0 4px;
}
.pay-name {
  font-size: 14px;
  color: #64748B;
  margin: 0 0 8px;
}
.pay-amount {
  font-size: 34px;
  font-weight: 800;
  color: #D97706;
  margin: 0 0 6px;
  line-height: 1.2;
}
.pay-meta {
  font-size: 13px;
  color: #94A3B8;
  margin: 0 0 14px;
}
.pay-tip {
  font-size: 12px;
  color: #94A3B8;
  background: #F8FAFC;
  border-radius: 8px;
  padding: 8px 12px;
  margin: 0;
}
.pay-tip i {
  margin-right: 4px;
  color: #1CB89D;
}
</style>
