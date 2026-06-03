<template>
  <div v-if="loading || text" class="ai-suggestion-box">
    <div class="ai-suggestion-header">
      <span>{{ title }}</span>
      <el-tag v-if="loading" type="info" effect="plain">{{
        loadingText
      }}</el-tag>
    </div>
    <el-skeleton v-if="loading" :rows="3" animated />
    <div v-else class="ai-suggestion-content">
      {{ text }}
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: "AI修改建议",
  },
  loadingText: {
    type: String,
    default: "AI生成中...",
  },
  loading: {
    type: Boolean,
    default: false,
  },
  text: {
    type: String,
    default: "",
  },
});
</script>

<style scoped lang="scss">
.ai-suggestion-box {
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid #dbe9ff;
  background: linear-gradient(135deg, #f7fbff 0%, #eef6ff 100%);
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.08);

  .ai-suggestion-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    span {
      font-size: 14px;
      font-weight: 600;
      color: #1f2d3d;
    }
  }

  .ai-suggestion-content {
    white-space: pre-wrap;
    line-height: 1.7;
    color: #3a4a5a;
    font-size: 14px;
  }
}
</style>
