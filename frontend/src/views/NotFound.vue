<template>
  <div class="notfound">
    <p class="code">404</p>
    <p class="text">抱歉，您访问的页面不存在</p>
    <el-button type="primary" @click="goHome" style="background: linear-gradient(135deg, #1CB89D, #0D9488); border: none; border-radius: 10px; padding: 10px 22px;">
      <i class="fa fa-home mr-2"></i>返回首页
    </el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function goHome() {
  router.replace(authStore.isAdmin ? '/travel' : '/travel/home')
}
</script>

<style scoped>
.notfound {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #F8FAFC;
}
.code {
  font-size: 72px;
  font-weight: 800;
  color: #0D9488;
  margin: 0;
  line-height: 1;
}
.text {
  font-size: 15px;
  color: #64748B;
  margin: 0 0 12px;
}
</style>
