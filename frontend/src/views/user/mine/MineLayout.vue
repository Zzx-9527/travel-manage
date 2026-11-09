<template>
  <div class="mine">
    <div class="mine-card">
      <!-- 左侧导航 -->
      <aside class="mine-side">
        <div class="side-profile">
          <el-avatar :size="64" :src="authStore.avatar" style="flex-shrink: 0; font-size: 24px;">
            <i class="fa fa-user"></i>
          </el-avatar>
          <p class="side-nickname">{{ authStore.nickname }}</p>
          <p class="side-role"><i class="fa fa-circle mr-1" style="font-size: 8px; color: #1CB89D;"></i>{{ authStore.roleName }}</p>
        </div>
        <nav class="side-nav">
          <router-link to="/travel/home/mine/profile" class="side-link" active-class="active">
            <i class="fa fa-user-circle mr-2"></i>个人资料
          </router-link>
          <router-link to="/travel/home/mine/orders" class="side-link" active-class="active">
            <i class="fa fa-ticket mr-2"></i>我的订单
          </router-link>
          <router-link to="/travel/home/mine/favorites" class="side-link" active-class="active">
            <i class="fa fa-heart mr-2"></i>我的收藏
          </router-link>
          <router-link to="/travel/home/mine/comments" class="side-link" active-class="active">
            <i class="fa fa-comments mr-2"></i>我的评论
          </router-link>
          <router-link to="/travel/home/mine/publish" class="side-link" active-class="active">
            <i class="fa fa-folder-open-o mr-2"></i>我的发布
          </router-link>
          <router-link to="/travel/home/publish" class="side-link" active-class="active">
            <i class="fa fa-pencil mr-2"></i>发布内容
          </router-link>
          <router-link to="/travel/home/messages" class="side-link" active-class="active">
            <i class="fa fa-bell-o mr-2"></i>消息中心
          </router-link>
        </nav>
      </aside>

      <!-- 右侧内容 -->
      <div class="mine-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>
.mine {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 56px;
  min-height: 60vh;
}

.mine-card {
  display: flex;
  gap: 24px;
  background: #F1F5F9;
  border-radius: 20px;
  padding: 24px;
}

/* 左侧 */
.mine-side {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 16px;
  padding: 24px 16px;
  align-self: flex-start;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  position: sticky;
  top: 88px;
}

.side-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px 0 20px;
  border-bottom: 1px solid #F1F5F9;
  margin-bottom: 12px;
}

.side-nickname {
  font-size: 16px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 8px 0 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-role {
  font-size: 12px;
  color: #94A3B8;
  margin: 0;
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.side-link {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #64748B;
  text-decoration: none;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.side-link:hover {
  background: #F8FAFC;
  color: #0D9488;
}

.side-link.active {
  background: #EBF8F5;
  color: #0D9488;
  font-weight: 600;
  border-left-color: #1CB89D;
}

.side-link i {
  width: 18px;
  text-align: center;
}

/* 右侧 */
.mine-content {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

@media (max-width: 860px) {
  .mine-card { flex-direction: column; padding: 16px; }
  .mine-side { width: 100%; position: static; }
  .side-profile { flex-direction: row; justify-content: flex-start; gap: 14px; }
  .side-nav { flex-direction: row; flex-wrap: wrap; }
}
</style>
