<template>
  <div class="page">
    <div v-if="loading" class="loading">
      <i class="fa fa-spinner fa-spin"></i>
    </div>

    <EmptyState v-else-if="degraded || !guide" title="攻略详情暂不可用" desc="站内进入即可查看，或稍后刷新重试" />

    <template v-else>
      <router-link to="/travel/home/guides" class="back-link">
        <i class="fa fa-arrow-left"></i>返回攻略列表
      </router-link>

      <div class="grid">
        <article class="card article">
          <h1 class="title">{{ guide.title }}</h1>

          <div class="byline">
            <el-avatar :size="32" :src="guide.userId?.avatar">
              <i class="fa fa-user"></i>
            </el-avatar>
            <span class="byline-name">{{ guide.userId?.nickname || '旅行者' }}</span>
            <span class="byline-time">{{ formatTime(guide.createTime) }}</span>

            <div class="meta">
              <span class="stat"><i class="fa fa-eye"></i>{{ guide.readCount || 0 }}</span>
              <span class="stat"><i class="fa fa-heart-o"></i>{{ guide.collectCount || 0 }}</span>
              <button class="collect" :class="{ collected: isCollected }" @click="toggleCollect">
                <i class="fa" :class="isCollected ? 'fa-heart' : 'fa-heart-o'"></i>
                {{ isCollected ? '已收藏' : '收藏' }}
              </button>
            </div>
          </div>

          <figure v-if="guide.cover" class="cover">
            <el-image :src="resolveUploadUrl(guide.cover)" fit="cover">
              <template #error>
                <div class="cover-fallback"><i class="fa fa-image"></i></div>
              </template>
            </el-image>
          </figure>

          <div class="prose">
            <p v-for="(para, i) in paragraphs" :key="i">{{ para }}</p>
          </div>

          <div class="end">全文完</div>
        </article>

        <aside class="side">
          <section class="card comments">
            <div class="c-head">
              <h2>评论</h2>
              <span class="count">{{ commentTotal }}</span>
            </div>

            <div class="cform">
              <div class="rate-row">
                <span>给你的评分</span>
                <el-rate v-model="myRating" :colors="['#F59E0B', '#F59E0B', '#F59E0B']" />
              </div>
              <el-input
                v-model="myContent"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="说说你的看法…"
              />
              <div class="submit-row">
                <button class="submit" :disabled="commentSubmitting" @click="submitComment">
                  <i v-if="commentSubmitting" class="fa fa-spinner fa-spin"></i>
                  {{ commentSubmitting ? '发表中' : '发表评论' }}
                </button>
              </div>
            </div>

            <div class="cbody">
              <div v-if="commentsLoading" class="state">
                <i class="fa fa-spinner fa-spin"></i>
              </div>

              <div v-else-if="commentsDegraded" class="state">
                <i class="fa fa-exclamation-circle"></i>
                <p>评论加载失败</p>
                <span>请稍后重试</span>
              </div>

              <div v-else-if="!commentList.length" class="state">
                <i class="fa fa-comment-o"></i>
                <p>还没有评论</p>
                <span>说说你的看法，帮到下一位旅行者</span>
              </div>

              <div v-else class="comment-list">
                <div v-for="c in commentList" :key="c.id" class="comment-item">
                  <el-avatar :size="32" :src="c.userId?.avatar">
                    <i class="fa fa-user"></i>
                  </el-avatar>
                  <div class="comment-body">
                    <div class="comment-head">
                      <span class="comment-user">{{ c.userId?.nickname || '匿名' }}</span>
                      <el-rate v-if="c.rating" :model-value="c.rating" disabled size="small" />
                    </div>
                    <p class="comment-content">{{ c.content }}</p>
                    <span class="comment-time">{{ formatTime(c.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { resolveUploadUrl } from '@/api/file'
import { ref, computed, onMounted } from 'vue'
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

// 正文按换行拆段：接口返回的是带 \n 的纯文本，拆开才能给段落之间留出行距
const paragraphs = computed(() =>
  (guide.value?.content || '')
    .split(/\n+/)
    .map((t) => t.trim())
    .filter(Boolean)
)

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
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 24px 64px;
  min-height: 60vh;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 96px 0;
  font-size: 26px;
  color: #0F766E;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748B;
  text-decoration: none;
  margin-bottom: 14px;
  transition: color 0.2s;
}
.back-link:hover { color: #0F766E; }

/* ===== 两栏 ===== */
.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: 20px;
}

.card {
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* ===== 正文卡 ===== */
.article {
  min-width: 0;
  padding: 34px 40px 38px;
}

.title {
  margin: 0 0 18px;
  font-size: 30px;
  line-height: 1.4;
  font-weight: 700;
  color: #1E293B;
  letter-spacing: 0.01em;
}

.byline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding-bottom: 18px;
  border-bottom: 1px solid #EEF2F6;
}

.byline-name {
  font-size: 13px;
  font-weight: 600;
  color: #1E293B;
}

.byline-time {
  font-size: 12px;
  color: #94A3B8;
}

.meta {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: #64748B;
  white-space: nowrap;
}
.stat i { color: #94A3B8; }

.collect {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #CCE7E2;
  background: #fff;
  color: #0F766E;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.collect:hover { background: #E8F5F2; border-color: #B7DED7; }
.collect.collected { background: #0F766E; border-color: #0F766E; color: #fff; }

/* 封面 */
.cover { margin: 22px 0 26px; }
.cover :deep(.el-image) {
  display: block;
  width: 100%;
  height: 330px;
  border-radius: 12px;
  overflow: hidden;
  background: #E2E8F0;
}
.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #EEF2F6;
  color: #CBD5E1;
  font-size: 26px;
}

/* 正文 */
.prose {
  font-size: 16px;
  line-height: 1.9;
  color: #334155;
}
.prose p { margin: 0 0 18px; }
.prose p:last-child { margin-bottom: 0; }

.end {
  margin-top: 32px;
  padding-top: 18px;
  border-top: 1px solid #EEF2F6;
  text-align: center;
  font-size: 11.5px;
  letter-spacing: 0.3em;
  color: #94A3B8;
}

/* ===== 评论卡 ===== */
.side {
  min-width: 0;
  display: flex;
}

.comments {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px 22px 22px;
}

.c-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.c-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1E293B;
}
.count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 6px;
  background: #F1F5F9;
  color: #64748B;
  font-size: 11.5px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.cform { margin-bottom: 18px; }

.rate-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 9px;
}
.rate-row > span { font-size: 12px; color: #64748B; }
.rate-row :deep(.el-rate) { height: 20px; }
.rate-row :deep(.el-rate__icon) { font-size: 16px; margin-right: 2px; }

.cform :deep(.el-textarea__inner) {
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 0 0 1px #E2E8F0 inset;
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.7;
  color: #1E293B;
  padding: 10px 12px;
  resize: vertical;
}
.cform :deep(.el-textarea__inner::placeholder) { color: #A8B4C2; }
.cform :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #0F766E inset, 0 0 0 3px rgba(15, 118, 110, 0.1);
}
.cform :deep(.el-input__count) {
  background: transparent;
  font-size: 10.5px;
  color: #94A3B8;
}

.submit-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.submit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border: 0;
  border-radius: 8px;
  background: #0F766E;
  color: #fff;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: background 0.2s;
}
.submit:hover { background: #0A665C; }
.submit[disabled] { opacity: 0.65; cursor: default; }

.cbody {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 空态 / 加载 / 失败：在剩余空间里居中 */
.state {
  margin: auto 0;
  text-align: center;
  padding: 30px 6px;
  color: #64748B;
}
.state > i { font-size: 22px; color: #CBD5E1; }
.state p { margin: 12px 0 4px; font-size: 13px; color: #64748B; }
.state span { font-size: 11.5px; line-height: 1.6; color: #94A3B8; }

/* 评论列表 */
.comment-list { display: flex; flex-direction: column; }

.comment-item {
  display: flex;
  gap: 10px;
  padding: 14px 0;
  border-top: 1px solid #F1F5F9;
}
.comment-list .comment-item:first-child { border-top: 0; padding-top: 0; }

.comment-body { flex: 1; min-width: 0; }

.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.comment-user { font-size: 12.5px; font-weight: 600; color: #1E293B; }
.comment-head :deep(.el-rate) { height: 14px; }
.comment-head :deep(.el-rate__icon) { font-size: 11px; margin-right: 1px; }

.comment-content {
  margin: 5px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-time {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: #94A3B8;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  /* 单栏时收窄整列，避免正文行长过宽影响阅读 */
  .grid { grid-template-columns: minmax(0, 1fr); max-width: 760px; margin: 0 auto; }
}

@media (max-width: 640px) {
  .page { padding: 16px 14px 52px; }
  .article { padding: 24px 20px 28px; }
  .title { font-size: 24px; }
  .cover :deep(.el-image) { height: 200px; }
  .meta { margin-left: 0; width: 100%; }
}
</style>
