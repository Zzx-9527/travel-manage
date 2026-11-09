<template>
  <div>
    <h2 class="page-title">我的订单</h2>

    <div v-if="loading" class="flex justify-center py-16">
      <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
    </div>

    <EmptyState v-else-if="degraded" title="我的订单加载失败" desc="请稍后重试，或联系管理员确认服务状态" />

    <el-empty v-else-if="!list.length" description="暂无订单，去发现景点吧～" :image-size="110">
      <router-link to="/travel/home/spots" class="go-btn">去逛逛景点</router-link>
    </el-empty>

    <div v-else class="order-list">
      <div v-for="order in list" :key="order.id" class="order-item">
        <div class="order-head">
          <span class="order-no"><i class="fa fa-ticket mr-1"></i>{{ order.orderNo || '--' }}</span>
          <span class="status-badge" :style="orderStatusStyle(order)">
            <i :class="orderStatusIcon(order)"></i>{{ orderStatusText(order) }}
          </span>
        </div>
        <div class="order-body">
          <div class="order-scenic">
            <p class="scenic-name">{{ order.scenicId?.name || '--' }}</p>
            <p class="scenic-date"><i class="fa fa-calendar mr-1"></i>游玩 {{ order.playDate || '--' }}</p>
          </div>
          <div class="order-info">
            <div class="info-cell">
              <p class="info-label">票数</p>
              <p class="info-value">{{ order.ticketCount ?? 0 }} 张</p>
            </div>
            <div class="info-cell">
              <p class="info-label">金额</p>
              <p class="info-value amount">¥{{ (order.totalAmount ?? 0).toLocaleString('zh-CN') }}</p>
            </div>
            <div class="info-cell">
              <p class="info-label">下单时间</p>
              <p class="info-value">{{ formatTime(order.createTime) }}</p>
            </div>
            <div class="info-cell">
              <p class="info-label">联系人</p>
              <p class="info-value">{{ order.contactName || '--' }}（{{ order.contactPhone || '--' }}）</p>
            </div>
          </div>
        </div>
        <div v-if="Number(order.status) === 0" class="order-actions">
          <span class="pay-deadline">请在 30 分钟内完成支付，超时自动取消</span>
          <el-button size="small" type="primary" @click="handlePay(order)" style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
            去支付
          </el-button>
          <el-button size="small" @click="handleCancel(order)" style="border-radius: 8px;">取消订单</el-button>
        </div>
        <div v-else-if="Number(order.status) === 2 && order.cancelTime" class="order-actions">
          <span class="pay-deadline">取消时间：{{ formatTime(order.cancelTime) }}</span>
        </div>
      </div>

      <div v-if="list.length" class="pager">
        <span class="pager-text">共 <b>{{ total }}</b> 条记录，第 {{ page }}/{{ totalPages }} 页</span>
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :page-sizes="[8, 16, 32]" :total="total" :pager-count="5"
          layout="sizes, prev, pager, next, jumper" background
          @size-change="setSize" @current-change="setPage"
        />
      </div>
    </div>

    <!-- 补支付：复用购票下单的模拟支付弹窗 -->
    <PayDialog
      v-model="payVisible"
      :name="payingOrder ? (payingOrder.scenicId?.name || payingOrder.orderNo || '') : ''"
      :amount="payingOrder ? (payingOrder.totalAmount ?? 0).toLocaleString('zh-CN') : '0'"
      :meta="payingOrder ? `${payingOrder.playDate || ''} · ${payingOrder.ticketCount ?? 0} 张门票` : ''"
      :loading="paying"
      @pay="confirmPay"
      @later="payVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '@/components/user/EmptyState.vue'
import { formatTime, orderStatusText, orderStatusIcon, orderStatusStyle } from '@/utils/format'
import { getMyOrders, payOrder, cancelOrder } from '@/api/order'
import PayDialog from '@/components/user/PayDialog.vue'

const list = ref([])
const total = ref(0)
const totalPages = ref(0)
const page = ref(1)
const size = ref(8)
const loading = ref(false)
const degraded = ref(false)

// 后端 queryByPage 已按 token 只返回本人订单，这里直接本地分页
async function fetchList() {
  loading.value = true
  try {
    const res = await getMyOrders({ page: 1, size: 200 })
    if (res.code === 1) {
      const all = res.content?.content || res.content || []
      total.value = all.length
      totalPages.value = Math.ceil(all.length / size.value)
      const start = (page.value - 1) * size.value
      list.value = all.slice(start, start + size.value)
      degraded.value = false
    }
  } catch (err) {
    degraded.value = true
  } finally {
    loading.value = false
  }
}

function setPage(p) {
  page.value = p
  fetchList()
}

function setSize(s) {
  size.value = s
  page.value = 1
  fetchList()
}

// 去支付：先弹模拟支付窗，确认后才真正调支付接口
const payVisible = ref(false)
const paying = ref(false)
const payingOrder = ref(null)

function handlePay(order) {
  payingOrder.value = order
  payVisible.value = true
}

async function confirmPay() {
  if (!payingOrder.value) return
  paying.value = true
  try {
    const res = await payOrder(payingOrder.value.id)
    if (res.code === 1) {
      ElMessage.success('支付成功')
      payVisible.value = false
      fetchList()
    }
  } catch (err) {
    // 拦截器已提示
  } finally {
    paying.value = false
  }
}

function handleCancel(order) {
  ElMessageBox.confirm(`确定要取消订单「${order.orderNo || order.id}」吗？取消后余票会立即回补，订单记录仍会保留。`, '提示', {
    confirmButtonText: '确定取消', cancelButtonText: '再想想', type: 'warning'
  }).then(async () => {
    try {
      const res = await cancelOrder(order.id)
      if (res.code === 1) {
        ElMessage.success('订单已取消，余票已回补')
        fetchList()
      }
    } catch (err) {
      // 拦截器已提示
    }
  }).catch(() => {})
}

onMounted(() => fetchList())
</script>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 24px;
}

.go-btn {
  display: inline-block;
  padding: 10px 24px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1CB89D, #0D9488);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.25s;
}

.go-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 15px rgba(28,184,157,0.4); }

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-item {
  background: #F8FAFC;
  border: 1px solid #F1F5F9;
  border-radius: 14px;
  padding: 18px 20px;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.order-no {
  font-family: ui-monospace, monospace;
  font-size: 13px;
  font-weight: 600;
  color: #1D4ED8;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.order-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.order-scenic {
  flex: 1;
  min-width: 200px;
}

.scenic-name {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  margin: 0 0 6px;
}

.scenic-date {
  font-size: 13px;
  color: #94A3B8;
  margin: 0;
}

.order-info {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.info-cell { min-width: 90px; }

.info-label {
  font-size: 12px;
  color: #94A3B8;
  margin: 0 0 4px;
}

.info-value {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  margin: 0;
}

.info-value.amount {
  color: #059669;
  font-weight: 700;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #E2E8F0;
}

.pay-deadline {
  margin-right: auto;
  font-size: 12px;
  color: #D97706;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.pager-text {
  font-size: 13px;
  color: #94A3B8;
}

.pager-text b { color: #475569; }
</style>
