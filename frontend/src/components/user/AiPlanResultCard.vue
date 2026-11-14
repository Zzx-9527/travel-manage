<template>
  <div class="plan-card">
    <div class="plan-head">
      <h3>{{ plan.title || '为你规划的行程' }}</h3>
      <p v-if="plan.intro">{{ plan.intro }}</p>
    </div>

    <div class="days">
      <div class="day" v-for="day in plan.days" :key="day.dayNo">
        <div class="day-head">
          <span class="day-no">D{{ day.dayNo }}</span>
          <span class="day-theme">{{ day.theme }}</span>
        </div>

        <div v-if="!day.spots || !day.spots.length" class="day-empty">自由活动 / 机动安排</div>

        <div v-for="spot in day.spots" :key="spot.scenicId" class="spot">
          <div class="spot-line">
            <span v-if="spot.timeSlot" class="slot" :class="slotClass(spot.timeSlot)">{{ spot.timeSlot }}</span>
            <a class="spot-name" @click="$emit('spot', spot.scenicId)">{{ spot.name }}</a>
            <span class="spot-meta">
              <span v-if="spot.price != null">¥{{ spot.price }}</span>
              <span v-if="spot.stayTime">{{ spot.stayTime }}</span>
            </span>
          </div>
          <p v-if="spot.intro" class="spot-intro">{{ spot.intro }}</p>
          <p v-if="spot.traffic" class="spot-note"><i>交通</i>{{ spot.traffic }}</p>
          <p v-if="spot.notice" class="spot-note"><i>提示</i>{{ spot.notice }}</p>
        </div>
      </div>
    </div>

    <div v-if="plan.tips && plan.tips.length" class="tips">
      <p v-for="(tip, index) in plan.tips" :key="index">· {{ tip }}</p>
    </div>

    <div class="plan-actions">
      <button class="act" :disabled="busy" @click="$emit('replan')">再生成</button>
      <button class="act" :disabled="busy" @click="$emit('to-map')">地图规划</button>
      <button class="act primary" :disabled="busy" @click="$emit('save')">保存为路线</button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  plan: { type: Object, required: true },
  busy: { type: Boolean, default: false }
})

defineEmits(['save', 'to-map', 'replan', 'spot'])

function slotClass(slot) {
  if (slot === '上午') return 'slot-morning'
  if (slot === '下午') return 'slot-afternoon'
  if (slot === '晚上') return 'slot-evening'
  return 'slot-unknown'
}
</script>

<style scoped>
.plan-card {
  background: #fff;
  border: 1px solid #e8eef5;
  border-radius: 14px;
  padding: 12px 14px;
  margin: 6px 0;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}

.plan-head h3 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.plan-head p {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #6b7280;
}

.day {
  border-top: 1px dashed #eef2f7;
  padding-top: 8px;
  margin-top: 8px;
}

.day-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.day-no {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: #1cb89d;
  border-radius: 5px;
  padding: 1px 6px;
}

.day-theme {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.day-empty {
  font-size: 12px;
  color: #9ca3af;
  padding: 2px 0;
}

.spot {
  margin-bottom: 10px;
}

.spot-line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.slot {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 999px;
}

.slot-morning { background: #fef3c7; color: #b45309; }
.slot-afternoon { background: #dbeafe; color: #1d4ed8; }
.slot-evening { background: #e0e7ff; color: #4338ca; }
.slot-unknown { background: #f3f4f6; color: #6b7280; }

.spot-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  cursor: pointer;
}

.spot-name:hover {
  color: #0f9481;
}

.spot-meta {
  margin-left: auto;
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #9ca3af;
}

.spot-intro {
  margin: 4px 0 2px;
  font-size: 12px;
  line-height: 1.65;
  color: #4b5563;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spot-note {
  margin: 2px 0 0;
  font-size: 11.5px;
  line-height: 1.55;
  color: #6b7280;
  display: flex;
  gap: 6px;
}

.spot-note i {
  flex-shrink: 0;
  font-style: normal;
  color: #9ca3af;
}

.tips {
  border-top: 1px dashed #eef2f7;
  margin-top: 8px;
  padding-top: 8px;
}

.tips p {
  margin: 0 0 2px;
  font-size: 11.5px;
  line-height: 1.6;
  color: #6b7280;
}

.plan-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.act {
  flex: 1;
  height: 30px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  color: #4b5563;
  cursor: pointer;
}

.act:hover {
  border-color: #1cb89d;
  color: #0f9481;
}

.act.primary {
  background: #1cb89d;
  border-color: #1cb89d;
  color: #fff;
}

.act.primary:hover {
  background: #0f9481;
}

.act:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
