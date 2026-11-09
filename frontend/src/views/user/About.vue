<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">关于平台</h2>
      <p class="page-desc">旅迹 · 旅游信息管理平台说明</p>
    </div>

    <!-- 锚点导航 -->
    <div class="anchor-nav">
      <button
        v-for="s in sections"
        :key="s.id"
        class="anchor-btn"
        :class="{ active: activeSection === s.id }"
        @click="goSection(s.id)"
      >
        {{ s.label }}
      </button>
    </div>

    <!-- 章节 -->
    <div class="section-card" id="section-about">
      <h3 class="section-title"><i class="fa fa-globe mr-2" style="color: #1CB89D;"></i>关于我们</h3>
      <p class="section-text">
        「旅迹」是一个旅游信息管理平台，面向游客提供景点浏览、在线购票、旅游攻略、路线规划等一站式旅行信息服务；
        同时为平台管理员提供用户、景点、攻略、路线、评论、收藏与订单等内容的统一管理能力。
      </p>
      <p class="section-text">
        本项目为毕业设计作品，前端基于 Vue 3 + Vite + Element Plus 构建，后端基于 Spring Boot 提供服务。
      </p>
    </div>

    <div class="section-card" id="section-help">
      <h3 class="section-title"><i class="fa fa-life-ring mr-2" style="color: #1CB89D;"></i>帮助中心</h3>
      <ul class="help-list">
        <li><b>如何购票？</b> 进入景点详情页，点击「在线购票」，选择游玩日期与门票数量，填写联系信息并确认下单即可。</li>
        <li><b>如何收藏攻略？</b> 打开任意攻略详情页，点击右上角「收藏」按钮；在「个人中心 → 我的收藏」中可查看或取消收藏。</li>
        <li><b>如何发表评论？</b> 在攻略详情页底部的评论区填写内容并选择评分，点击「发表评论」即可，评论需管理员审核后展示。</li>
        <li><b>如何查看订单？</b> 在「个人中心 → 我的订单」中可查看订单状态，未支付订单可进行支付或取消操作。</li>
        <li><b>忘记密码怎么办？</b> 可在「个人中心 → 个人资料」中通过原密码修改密码；若忘记原密码，请联系平台管理员重置。</li>
      </ul>
    </div>

    <div class="section-card" id="section-privacy">
      <h3 class="section-title"><i class="fa fa-lock mr-2" style="color: #1CB89D;"></i>隐私政策</h3>
      <p class="section-text">
        我们重视并保护您的个人隐私。平台仅在为您提供服务所必需的范围内收集、使用您的个人信息，包括但不限于账号信息、
        联系方式与订单信息，并采取合理的安全措施防止信息泄露、丢失或被篡改。
      </p>
      <p class="section-text">
        除法律法规另有规定或经您明确同意外，我们不会向任何第三方提供您的个人信息。本平台为教学演示性质的项目，
        请勿在其中提交真实敏感信息。
      </p>
    </div>

    <div class="section-card" id="section-terms">
      <h3 class="section-title"><i class="fa fa-file-text-o mr-2" style="color: #1CB89D;"></i>用户协议</h3>
      <p class="section-text">
        使用本平台即表示您同意遵守以下条款：您保证所填写的账号与信息真实、合法、有效；不得利用平台从事任何违法违规活动，
        不得恶意攻击、干扰平台正常运行。
      </p>
      <p class="section-text">
        平台上景点、攻略与路线等内容由管理员或用户发布，仅供学习交流使用，平台对其真实性与准确性不承担担保责任。
        如内容侵犯您的合法权益，请联系管理员进行处理。
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const sections = [
  { id: 'about', label: '关于我们' },
  { id: 'help', label: '帮助中心' },
  { id: 'privacy', label: '隐私政策' },
  { id: 'terms', label: '用户协议' }
]

const activeSection = ref('about')

function goSection(id) {
  activeSection.value = id
  document.getElementById(`section-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(() => {
  const s = route.query.s
  if (s && sections.some(x => x.id === s)) {
    // 等渲染完成后定位到对应章节
    setTimeout(() => goSection(s), 60)
  }
})
</script>

<style scoped>
.page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px 56px;
  min-height: 60vh;
}

.page-head {
  margin-bottom: 20px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 6px;
}

.page-desc {
  font-size: 14px;
  color: #94A3B8;
  margin: 0;
}

/* 锚点导航 */
.anchor-nav {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 14px;
  padding: 12px 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  margin-bottom: 20px;
  position: sticky;
  top: 80px;
  z-index: 10;
}

.anchor-btn {
  padding: 8px 18px;
  border-radius: 10px;
  border: 1px solid #E2E8F0;
  background: #fff;
  color: #64748B;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.anchor-btn:hover {
  border-color: #1CB89D;
  color: #0D9488;
}

.anchor-btn.active {
  background: linear-gradient(135deg, #1CB89D, #0D9488);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 15px rgba(28, 184, 157, 0.3);
}

/* 章节卡片 */
.section-card {
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  margin-bottom: 18px;
  scroll-margin-top: 140px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 14px;
}

.section-text {
  font-size: 14px;
  color: #475569;
  line-height: 1.9;
  margin: 0 0 10px;
}

.section-text:last-child { margin-bottom: 0; }

.help-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.help-list li {
  font-size: 14px;
  color: #475569;
  line-height: 1.8;
}

.help-list b {
  color: #0D9488;
}
</style>
