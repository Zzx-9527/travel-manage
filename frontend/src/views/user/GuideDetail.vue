<template>
  <div class="page">
    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center py-20">
      <i class="fa fa-spinner fa-spin text-3xl" style="color: #D97706;"></i>
    </div>

    <!-- 降级空态 -->
    <EmptyState v-else-if="degraded || !guide" title="攻略详情暂不可用" desc="站内进入即可查看，或稍后刷新重试" />

    <template v-else>
      <!-- 返回 -->
      <router-link to="/travel/home/guides" class="back-link">
        <i class="fa fa-arrow-left mr-1"></i>返回攻略列表
      </router-link>

      <!-- 文章头 -->
      <header class="article-head">
        <div class="head-kicker"><span class="kicker-line"></span>旅行攻略 · TRAVEL GUIDE</div>
        <h1 class="head-title">{{ guide.title }}</h1>
        <div class="head-meta">
          <div class="author">
            <el-avatar :size="34" :src="guide.userId?.avatar" style="flex-shrink: 0;">
              <i class="fa fa-user"></i>
            </el-avatar>
            <div class="author-info">
              <span class="author-name">{{ guide.userId?.nickname || '旅行者' }}</span>
              <span class="author-time">{{ formatTime(guide.createTime) }}</span>
            </div>
          </div>
          <div class="head-stats">
            <span class="stat"><i class="fa fa-eye"></i>{{ guide.readCount || 0 }}</span>
            <span class="stat"><i class="fa fa-heart"></i>{{ guide.collectCount || 0 }}</span>
          </div>
          <button class="collect-btn" :class="{ collected: isCollected }" @click="toggleCollect">
            <i class="fa mr-1" :class="isCollected ? 'fa-heart' : 'fa-heart-o'"></i>
            {{ isCollected ? '已收藏' : '收藏' }}
          </button>
        </div>
        <el-image :src="resolveUploadUrl(guide.cover)" fit="cover" class="head-cover">
          <template #error><div class="head-cover-fallback"><i class="fa fa-image"></i></div></template>
        </el-image>
      </header>

      <!-- 正文 -->
      <article class="article-body">
        <p class="article-text">{{ guide.content }}</p>
        <div class="article-end">
          <span class="end-mark">·</span> 全文完 <span class="end-mark">·</span>
        </div>
      </article>

      <!-- 评论区 -->
      <section class="comments-card">
        <h3 class="comments-title"><i class="fa fa-comments mr-2"></i>评论区 · {{ commentTotal }}</h3>

        <!-- 写评论 -->
        <div class="comment-form">
          <div class="form-top">
            <span class="form-hint">分享你的看法</span>
            <el-rate v-model="myRating" :colors="['#F59E0B', '#F59E0B', '#F59E0B']" />
          </div>
          <el-input v-model="myContent" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="写下你的评论..." />
          <div class="form-submit">
            <el-button type="primary" :loading="commentSubmitting" @click="submitComment" style="border-radius: 10px; background: linear-gradient(135deg, #D97706, #B45309); border: none;">
              发表评论
            </el-button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div v-if="commentsLoading" class="flex justify-center py-8">
          <i class="fa fa-spinner fa-spin text-2xl" style="color: #D97706;"></i>
        </div>
        <EmptyState v-else-if="commentsDegraded" title="评论加载失败" desc="请稍后重试，或联系管理员确认服务状态" />
        <el-empty v-else-if="!commentList.length" description="还没有评论，来抢沙发～" :image-size="90" />
        <div v-else class="comment-list">
          <div v-for="c in commentList" :key="c.id" class="comment-item">
            <el-avatar :size="38" :src="c.userId?.avatar" style="flex-shrink: 0;">
              <i class="fa fa-user"></i>
            </el-avatar>
            <div class="comment-body">
              <div class="comment-head">
                <span class="comment-user">{{ c.userId?.nickname || '匿名' }}</span>
                <el-rate v-if="c.rating" :model-value="c.rating" disabled size="small" style="height: auto;" />
                <span class="comment-time">{{ formatTime(c.createTime) }}</span>
              </div>
              <p class="comment-content">{{ c.content }}</p>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { resolveUploadUrl } from '@/api/file'
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/user/EmptyState.vue'
import { formatTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { getStrategyDetail } from '@/api/strategy'
import { getCommentsByStrategy, addComment } from '@/api/comment'
import { checkCollect, addCollect, deleteCollectByTarget } from '@/api/collect'

const route = useRoute()
const authStore = useAuthStore()

const guide = ref(null)
const loading = ref(true)
const degraded = ref(false)

// 收藏
const isCollected = ref(false)
const collectBusy = ref(false)

// 评论
const commentList = ref([])
const commentTotal = ref(0)
const commentsLoading = ref(true)
const commentsDegraded = ref(false)
const myContent = ref('')
const myRating = ref(5)
const commentSubmitting = ref(false)

async function resolveGuide() {
  // 仅当缓存对象带正文等完整字段时才使用，否则（如从“我的评论”跳来）走接口补全
  const cached = history.state?.guide
  if (route.params.id && cached?.id === Number(route.params.id) && cached?.content != null) {
    guide.value = cached
    loading.value = false
    await Promise.all([checkFavorite(), loadComments()])
    return
  }
  try {
    const res = await getStrategyDetail(route.params.id)
    if (res.code === 1) {
      guide.value = res.content
      await Promise.all([checkFavorite(), loadComments()])
    } else {
      degraded.value = true
    }
  } catch (err) {
    degraded.value = true
  } finally {
    loading.value = false
  }
}

async function checkFavorite() {
  const uid = authStore.userInfo?.id
  if (!uid) { isCollected.value = false; return }
  try {
    const res = await checkCollect({ targetType: 1, targetId: guide.value.id })
    if (res.code === 1) isCollected.value = !!res.content
  } catch (err) {
    isCollected.value = false
  }
}

async function toggleCollect() {
  const uid = authStore.userInfo?.id
  if (!uid) { ElMessage.warning('请先登录'); return }
  if (collectBusy.value) return
  collectBusy.value = true
  try {
    const res = isCollected.value
      ? await deleteCollectByTarget({ targetType: 1, targetId: guide.value.id })
      : await addCollect({ targetType: 1, targetId: guide.value.id })
    if (res.code === 1) {
      isCollected.value = !isCollected.value
      guide.value.collectCount = (guide.value.collectCount || 0) + (isCollected.value ? 1 : -1)
      ElMessage.success(isCollected.value ? '收藏成功' : '已取消收藏')
    }
  } catch (err) {
    // 后端已实现，失败时由拦截器统一提示
  } finally {
    collectBusy.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true
  try {
    // 服务端按当前攻略 + 已通过审核过滤，真分页；不再拉全站前 200 条再前端筛
    const res = await getCommentsByStrategy({
      strategyId: guide.value.id,
      status: 1,
      page: 1,
      size: 20
    })
    if (res.code === 1) {
      commentList.value = res.content?.content || []
      commentTotal.value = res.content?.totalElements ?? commentList.value.length
    }
  } catch (err) {
    commentsDegraded.value = true
  } finally {
    commentsLoading.value = false
  }
}

async function submitComment() {
  if (!myContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    const res = await addComment({
      strategyId: { id: guide.value.id },
      content: myContent.value.trim(),
      rating: myRating.value
    })
    if (res.code === 1) {
      ElMessage.success('评论发表成功，等待审核')
      myContent.value = ''
      myRating.value = 5
      loadComments()
    }
  } catch (err) {
    // 失败时由拦截器统一提示
  } finally {
    commentSubmitting.value = false
  }
}

onMounted(resolveGuide)
</script>

<style scoped>
.page {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 24px 56px;
  min-height: 60vh;
}

.back-link {
  display: inline-block;
  font-size: 13px;
  color: #64748B;
  text-decoration: none;
  margin-bottom: 16px;
  transition: color 0.2s;
}

.back-link:hover { color: #B45309; }

/* ===== 文章头 ===== */
.article-head {
  background: #FFFDF8;
  border: 1px solid #F1E8D8;
  border-radius: 20px;
  padding: 30px 32px 26px;
  box-shadow: 0 8px 24px rgba(120, 84, 30, 0.08);
  margin-bottom: 18px;
}

.head-kicker {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #B45309;
  margin-bottom: 14px;
}

.kicker-line {
  width: 32px;
  height: 3px;
  border-radius: 3px;
  background: linear-gradient(90deg, #D97706, #FBBF24);
}

.head-title {
  font-size: 30px;
  font-weight: 800;
  color: #1C1917;
  margin: 0 0 20px;
  line-height: 1.35;
  letter-spacing: 0.5px;
}

.head-meta {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-info {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.author-name {
  font-size: 13px;
  font-weight: 600;
  color: #44403C;
}

.author-time {
  font-size: 12px;
  color: #A8A29E;
}

.head-stats {
  display: flex;
  gap: 14px;
  margin-left: auto;
}

.stat {
  font-size: 13px;
  color: #78716C;
}
.stat i { color: #D97706; margin-right: 5px; }

.collect-btn {
  padding: 9px 20px;
  border-radius: 999px;
  border: 1px solid #FECACA;
  background: #FFF1F2;
  color: #E11D48;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}

.collect-btn:hover { transform: translateY(-1px); }

.collect-btn.collected {
  background: linear-gradient(135deg, #FB7185, #E11D48);
  border: none;
  color: #fff;
  box-shadow: 0 4px 15px rgba(225, 29, 72, 0.28);
}

.head-cover {
  width: 100%;
  height: 260px;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(120, 84, 30, 0.12);
}

.head-cover-fallback {
  width: 100%; height: 100%;
  background: #F3EDE0;
  display: flex; align-items: center; justify-content: center;
  color: #C4B79D;
  font-size: 28px;
}

/* ===== 正文 ===== */
.article-body {
  background: #FFFDF8;
  border: 1px solid #F1E8D8;
  border-radius: 20px;
  padding: 34px 40px;
  box-shadow: 0 8px 24px rgba(120, 84, 30, 0.08);
  margin-bottom: 18px;
}

.article-text {
  font-size: 16px;
  color: #292524;
  line-height: 2.1;
  white-space: pre-wrap;
  word-break: break-word;
}

.article-text::first-letter {
  font-size: 44px;
  font-weight: 800;
  color: #B45309;
  float: left;
  line-height: 1;
  padding-right: 8px;
  margin-top: 4px;
}

.article-end {
  text-align: center;
  color: #C4B79D;
  font-size: 13px;
  letter-spacing: 3px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px dashed #E8DCC6;
}

.end-mark { color: #D97706; }

/* ===== 评论区 ===== */
.comments-card {
  background: #FFFDF8;
  border: 1px solid #F1E8D8;
  border-radius: 20px;
  padding: 24px 28px;
  box-shadow: 0 8px 24px rgba(120, 84, 30, 0.08);
}

.comments-title {
  font-size: 16px;
  font-weight: 700;
  color: #1C1917;
  margin: 0 0 18px;
}
.comments-title i { color: #D97706; }

.comment-form {
  background: #F7F1E4;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 20px;
}

.form-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.form-hint {
  font-size: 13px;
  font-weight: 600;
  color: #B45309;
}

.form-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #F1E8D8;
}

.comment-item:last-child { border-bottom: none; }

.comment-body { flex: 1; min-width: 0; }

.comment-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.comment-user {
  font-size: 14px;
  font-weight: 600;
  color: #1C1917;
}

.comment-time {
  font-size: 12px;
  color: #A8A29E;
}

.comment-content {
  font-size: 14px;
  color: #44403C;
  line-height: 1.7;
  margin: 6px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 640px) {
  .article-body { padding: 24px 20px; }
  .head-title { font-size: 24px; }
  .head-cover { height: 200px; }
  .head-stats { margin-left: 0; }
}
</style>
