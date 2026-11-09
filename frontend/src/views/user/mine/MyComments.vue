<template>
  <div>
    <h2 class="page-title">我的评论</h2>

    <div v-if="loading" class="flex justify-center py-16">
      <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
    </div>

    <EmptyState v-else-if="degraded" title="我的评论加载失败" desc="请稍后重试，或联系管理员确认服务状态" />

    <el-empty v-else-if="!list.length" description="还没有发表过评论" :image-size="110">
      <router-link to="/travel/home/guides" class="go-btn">去逛逛攻略</router-link>
    </el-empty>

    <div v-else class="comment-list">
      <div v-for="c in list" :key="c.id" class="comment-item">
        <div class="comment-main">
          <p class="comment-content">{{ c.content }}</p>
          <div class="comment-meta">
            <span class="guide-link" @click="viewGuide(c.strategyId)"><i class="fa fa-book mr-1"></i>{{ c.strategyId?.title || '未知攻略' }}</span>
            <el-rate v-if="c.rating" :model-value="c.rating" disabled size="small" style="height: auto;" />
            <span class="status-badge" :style="auditStyle(c.status)">
              <i :class="auditIcon(c.status)"></i>{{ auditText(c.status) }}
            </span>
            <span class="comment-time">{{ formatTime(c.createTime) }}</span>
          </div>
        </div>
        <button class="del-btn" @click="removeComment(c)"><i class="fa fa-trash-o"></i></button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/user/EmptyState.vue'
import { formatTime, auditText, auditIcon, auditStyle } from '@/utils/format'
import { getMyComments, deleteMyComment } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const list = ref([])
const total = ref(0)
const totalPages = ref(0)
const page = ref(1)
const size = ref(8)
const loading = ref(false)
const degraded = ref(false)

// 后端 queryByPage 不按用户过滤，取全量后在前端按 userId 过滤 + 本地分页
async function fetchList() {
  const uid = authStore.userInfo?.id
  if (!uid) {
    list.value = []
    total.value = 0
    totalPages.value = 0
    return
  }
  loading.value = true
  try {
    const res = await getMyComments({ page: 1, size: 200 })
    if (res.code === 1) {
      const all = res.content?.content || res.content || []
      const mine = all.filter(c => Number(c.userId?.id) === Number(uid))
      total.value = mine.length
      totalPages.value = Math.ceil(mine.length / size.value)
      const start = (page.value - 1) * size.value
      list.value = mine.slice(start, start + size.value)
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

function viewGuide(strategyId) {
  if (!strategyId?.id) return
  router.push({ path: `/travel/home/guides/${strategyId.id}`, state: { guide: strategyId } })
}

async function removeComment(c) {
  try {
    const res = await deleteMyComment(c.id)
    if (res.code === 1) {
      ElMessage.success('评论已删除')
      list.value = list.value.filter(item => item.id !== c.id)
    }
  } catch (err) {
    // 拦截器已提示
  }
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

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: #F8FAFC;
  border: 1px solid #F1F5F9;
  border-radius: 14px;
  padding: 16px 18px;
}

.comment-main { flex: 1; min-width: 0; }

.comment-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.7;
  margin: 0 0 10px;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.guide-link {
  font-size: 13px;
  color: #2563EB;
  cursor: pointer;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-link:hover { text-decoration: underline; }

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.comment-time {
  font-size: 12px;
  color: #94A3B8;
  margin-left: auto;
}

.del-btn {
  flex-shrink: 0;
  width: 32px; height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94A3B8;
  cursor: pointer;
  transition: all 0.2s;
}

.del-btn:hover { background: #FEE2E2; color: #DC2626; }

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
