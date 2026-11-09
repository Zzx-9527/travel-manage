<template>
  <transition name="banner-slide">
    <div v-if="visible && announcement" class="announce-banner">
      <div class="banner-inner">
        <i class="fa fa-bullhorn banner-horn"></i>
        <div class="banner-text">
          <span class="banner-title">{{ announcement.title }}</span>
          <span class="banner-content">{{ announcement.content }}</span>
        </div>
        <button class="banner-close" title="关闭" @click="close"><i class="fa fa-times"></i></button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMessagesByPage, BANNER_ADMIN_ID } from '@/api/message'

// 全站横幅：管理员（id=10）的消息都视为系统公告，
// 取其最新一条在用户端顶部展示；关闭后本次会话（sessionStorage）不再打扰
const announcement = ref(null)
const visible = ref(false)

async function load() {
  try {
    const res = await getMessagesByPage({ page: 1, size: 20, userId: BANNER_ADMIN_ID })
    const rows = res.content?.content || []
    const latest = [...rows].sort((a, b) => (b.id ?? 0) - (a.id ?? 0))[0]
    if (latest && sessionStorage.getItem('announce_closed_' + latest.id) !== '1') {
      announcement.value = latest
      visible.value = true
    }
  } catch (e) { /* 公告获取失败静默，不影响页面 */ }
}

function close() {
  if (announcement.value) {
    sessionStorage.setItem('announce_closed_' + announcement.value.id, '1')
  }
  visible.value = false
}

onMounted(load)
</script>

<style scoped>
.announce-banner {
  background: linear-gradient(135deg, #0D1B2A 0%, #14324B 60%, #14525C 100%);
  color: #fff;
}

.banner-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 14px 48px 14px 24px;
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
}

.banner-horn {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

.banner-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.banner-title {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.banner-content {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner-close {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 30px; height: 30px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.55);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.banner-close:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.banner-slide-enter-active, .banner-slide-leave-active {
  transition: all 0.35s ease;
}
.banner-slide-enter-from, .banner-slide-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

@media (max-width: 600px) {
  .banner-content { white-space: normal; }
  .banner-inner { padding: 12px 44px 12px 16px; }
}
</style>
