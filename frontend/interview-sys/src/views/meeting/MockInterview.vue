<template>
  <div class="mock-interview-page">
    <div class="mock-shell">
      <header class="mock-header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
          <div class="header-text">
            <h1>AI模拟面试</h1>
            <p>{{ jobSummary }}</p>
          </div>
        </div>
      </header>

      <main class="mock-main">
        <section v-if="!interviewStarted" class="difficulty-panel">
          <h2>请选择面试难度</h2>
          <p class="difficulty-desc">
            难度将决定题目数量，开始后会按轮次进行问答。
          </p>

          <div class="difficulty-list">
            <button
              v-for="item in difficultyOptions"
              :key="item.value"
              type="button"
              class="difficulty-item"
              :class="{ active: selectedDifficulty === item.value }"
              @click="selectedDifficulty = item.value"
            >
              <div class="difficulty-title">{{ item.label }}</div>
              <div class="difficulty-meta">{{ item.rangeText }}</div>
            </button>
          </div>

          <el-button type="primary" size="large" @click="startInterview">
            开始模拟面试
          </el-button>
        </section>

        <section v-else class="chat-panel">
          <div class="chat-status">
            <span>难度：{{ currentDifficultyLabel }}</span>
            <span>进度：{{ answeredCount }}/{{ totalQuestions }}</span>
            <el-button link type="primary" @click="resetInterview"
              >重新开始</el-button
            >
          </div>

          <el-scrollbar ref="scrollbarRef" class="message-scrollbar">
            <div class="message-list">
              <div
                v-for="message in messages"
                :key="message.id"
                class="message-row"
                :class="message.role"
              >
                <div class="message-avatar">
                  <el-avatar
                    :size="36"
                    :icon="message.role === 'assistant' ? ChatDotRound : User"
                  />
                </div>
                <div class="message-bubble-wrap">
                  <div class="message-name">
                    {{ message.role === "assistant" ? "面试官" : "我" }}
                  </div>
                  <div class="message-bubble">
                    <div class="message-content">{{ message.content }}</div>
                  </div>
                </div>
              </div>

              <div v-if="replyLoading" class="message-row assistant">
                <div class="message-avatar">
                  <el-avatar :size="36" :icon="ChatDotRound" />
                </div>
                <div class="message-bubble-wrap">
                  <div class="message-name">面试官</div>
                  <div class="message-bubble typing-bubble">
                    <span class="typing-dot"></span>
                    <span class="typing-dot"></span>
                    <span class="typing-dot"></span>
                  </div>
                </div>
              </div>
            </div>
          </el-scrollbar>

          <div v-if="interviewFinished" class="result-card">
            <h3>模拟面试已结束</h3>
            <div class="score">综合得分：{{ finalScore }} 分</div>
            <p>{{ finalSummary }}</p>
            <el-button type="primary" @click="resetInterview"
              >返回难度选择，重新开始</el-button
            >
          </div>

          <div v-else class="composer">
            <div class="quick-actions">
              <el-tag
                v-for="chip in quickReplies"
                :key="chip"
                effect="plain"
                class="quick-chip"
                @click="fillQuickReply(chip)"
              >
                {{ chip }}
              </el-tag>
            </div>

            <el-input
              v-model="draft"
              type="textarea"
              :rows="3"
              resize="none"
              maxlength="1000"
              show-word-limit
              placeholder="输入你的回答，按 Enter 发送，Shift+Enter 换行"
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.enter.shift.stop
            />

            <div class="composer-actions">
              <span class="tip-text">回答当前问题后将进入下一题或生成评分</span>
              <el-button
                type="primary"
                :loading="replyLoading"
                @click="sendMessage"
                >发送</el-button
              >
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowLeft, ChatDotRound, User } from "@element-plus/icons-vue";
import { getJobPositionById } from "@/api/job";
import { handleMockInterview } from "@/api/mockInterview";

const router = useRouter();
const route = useRoute();
const scrollbarRef = ref(null);

const jobDetail = ref(null);
const messages = ref([]);
const draft = ref("");
const replyLoading = ref(false);
const interviewStarted = ref(false);
const interviewFinished = ref(false);

const selectedDifficulty = ref("MEDIUM");
const totalQuestions = ref(0);
const answeredCount = ref(0);
const currentQuestion = ref("");
const records = ref([]);
const finalScore = ref(0);
const finalSummary = ref("");

const difficultyOptions = [
  { value: "EASY", label: "简单", min: 1, max: 2, rangeText: "1-2个问题" },
  { value: "MEDIUM", label: "中等", min: 3, max: 4, rangeText: "3-4个问题" },
  { value: "HARD", label: "困难", min: 5, max: 6, rangeText: "5-6个问题" },
];

const quickReplies = [
  "我先做个简短自我介绍",
  "这个问题我会从背景、行动、结果三个方面回答",
  "我可以结合一个真实项目案例说明",
  "我的核心优势是快速学习和推进落地",
];

const jobSummary = computed(() => {
  if (!jobDetail.value) return "通用模拟面试";
  return `${jobDetail.value.companyName || "未知公司"} · ${jobDetail.value.title || "未命名职位"}`;
});

const currentDifficultyLabel = computed(() => {
  const hit = difficultyOptions.find(
    (item) => item.value === selectedDifficulty.value,
  );
  return hit?.label || "中等";
});

const goBack = () => {
  router.back();
};

const scrollToBottom = async () => {
  await nextTick();
  scrollbarRef.value?.setScrollTop?.(999999);
};

const addMessage = (role, content) => {
  messages.value.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
  });
  scrollToBottom();
};

const pickQuestionCount = (difficulty) => {
  const config =
    difficultyOptions.find((item) => item.value === difficulty) ||
    difficultyOptions[1];
  return Math.floor(Math.random() * (config.max - config.min + 1)) + config.min;
};

const buildPayload = () => {
  const job = jobDetail.value || {};
  return {
    difficulty: selectedDifficulty.value,
    totalQuestions: totalQuestions.value,
    companyName: job.companyName || "",
    jobTitle: job.title || "",
    workCity: job.workCity || "",
    description: job.description || "",
    requirement: job.requirement || "",
    records: records.value,
  };
};

const requestNextOrScore = async () => {
  const res = await handleMockInterview(buildPayload());
  const data = res?.data?.data;
  if (!data) {
    throw new Error("模拟面试服务暂不可用");
  }

  answeredCount.value = Number(data.answeredCount || records.value.length);
  if (data.finished) {
    interviewFinished.value = true;
    finalScore.value = Number(data.score || 0);
    finalSummary.value = data.summary || "本次模拟面试已结束。";
    addMessage(
      "assistant",
      `面试结束。你的综合得分是 ${finalScore.value} 分。${finalSummary.value}`,
    );
    currentQuestion.value = "";
    return;
  }

  const nextQuestion = data.nextQuestion || "请你继续阐述你的岗位匹配度。";
  currentQuestion.value = nextQuestion;
  addMessage("assistant", nextQuestion);
};

const startInterview = async () => {
  interviewStarted.value = true;
  interviewFinished.value = false;
  messages.value = [];
  records.value = [];
  draft.value = "";
  finalScore.value = 0;
  finalSummary.value = "";
  currentQuestion.value = "";
  answeredCount.value = 0;
  totalQuestions.value = pickQuestionCount(selectedDifficulty.value);

  replyLoading.value = true;
  try {
    await requestNextOrScore();
  } catch (error) {
    ElMessage.error(error?.message || "启动模拟面试失败，请稍后重试");
    resetInterview();
  } finally {
    replyLoading.value = false;
  }
};

const resetInterview = () => {
  interviewStarted.value = false;
  interviewFinished.value = false;
  messages.value = [];
  records.value = [];
  draft.value = "";
  currentQuestion.value = "";
  answeredCount.value = 0;
  totalQuestions.value = 0;
  finalScore.value = 0;
  finalSummary.value = "";
};

const fillQuickReply = (text) => {
  draft.value = text;
};

const sendMessage = async () => {
  const content = draft.value.trim();
  if (!content || replyLoading.value || interviewFinished.value) return;
  if (!currentQuestion.value) {
    ElMessage.warning("当前没有可回答的问题，请稍候");
    return;
  }

  addMessage("user", content);
  records.value.push({
    question: currentQuestion.value,
    answer: content,
  });
  draft.value = "";

  replyLoading.value = true;
  try {
    await requestNextOrScore();
  } catch (error) {
    ElMessage.error(error?.message || "AI 回复失败，请稍后重试");
    addMessage("assistant", "我暂时无法继续面试，请稍后重试。");
  } finally {
    replyLoading.value = false;
  }
};

const loadJobDetail = async () => {
  const jobId = route.query.jobId;
  if (!jobId) {
    jobDetail.value = null;
    return;
  }
  try {
    const res = await getJobPositionById(jobId);
    jobDetail.value = res?.data?.data || null;
  } catch (error) {
    jobDetail.value = null;
    ElMessage.error("获取职位信息失败，已切换到通用模式");
  }
};

onMounted(async () => {
  await loadJobDetail();
});
</script>

<style scoped>
.mock-interview-page {
  min-height: 100vh;
  padding: 20px;
  background:
    radial-gradient(
      circle at top left,
      rgba(22, 119, 255, 0.18),
      transparent 28%
    ),
    radial-gradient(
      circle at bottom right,
      rgba(88, 183, 255, 0.14),
      transparent 24%
    ),
    linear-gradient(180deg, #f5f9ff 0%, #eef4fb 100%);
}

.mock-shell {
  max-width: 1180px;
  margin: 0 auto;
  height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 20px 60px rgba(26, 54, 93, 0.12);
  backdrop-filter: blur(10px);
}

.mock-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.96),
    rgba(245, 249, 255, 0.9)
  );
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-text h1 {
  margin: 0;
  font-size: 22px;
  color: #10233f;
}

.header-text p {
  margin: 4px 0 0;
  color: #5b6b84;
  font-size: 13px;
}

.header-badge {
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12px;
  color: #0f4c81;
  background: #eaf3ff;
  border: 1px solid #cfe1ff;
  letter-spacing: 0.06em;
}

.mock-main {
  flex: 1;
  min-height: 0;
  padding: 18px;
}

.difficulty-panel {
  height: 100%;
  border-radius: 22px;
  padding: 30px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.2);
  display: flex;
  flex-direction: column;
  gap: 16px;
  justify-content: center;
  align-items: center;
}

.difficulty-panel h2 {
  margin: 0;
  font-size: 28px;
  color: #10233f;
}

.difficulty-desc {
  margin: 0;
  color: #5b6b84;
}

.difficulty-list {
  width: min(760px, 100%);
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin: 14px 0 6px;
}

.difficulty-item {
  border: 1px solid #cfe1ff;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  text-align: left;
}

.difficulty-item.active {
  border-color: #1677ff;
  box-shadow: 0 8px 20px rgba(22, 119, 255, 0.16);
  background: #f4f9ff;
}

.difficulty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.difficulty-meta {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.chat-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.92),
    rgba(248, 250, 252, 0.98)
  );
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.chat-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  color: #52637f;
  font-size: 13px;
}

.message-scrollbar {
  flex: 1;
  min-height: 0;
  padding: 24px 18px 10px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-bubble-wrap {
  max-width: min(720px, calc(100% - 60px));
}

.message-name {
  margin-bottom: 6px;
  font-size: 12px;
  color: #64748b;
}

.message-row.user .message-name {
  text-align: right;
}

.message-bubble {
  position: relative;
  padding: 14px 16px;
  border-radius: 18px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.message-row.assistant .message-bubble {
  background: #ffffff;
  color: #1f2937;
  border: 1px solid rgba(96, 165, 250, 0.14);
  box-shadow: 0 8px 24px rgba(91, 110, 145, 0.08);
}

.message-row.user .message-bubble {
  background: linear-gradient(135deg, #1677ff, #4f9dff);
  color: #fff;
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.18);
}

.typing-bubble {
  width: 74px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8aa9d6;
  animation: pulse 1.2s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.3s;
}

.result-card {
  margin: 10px 16px 16px;
  border-radius: 14px;
  border: 1px solid #d8e9ff;
  background: #f8fbff;
  padding: 16px;
}

.result-card h3 {
  margin: 0;
  color: #1a365d;
}

.result-card .score {
  margin: 10px 0;
  color: #0f4c81;
  font-size: 22px;
  font-weight: 700;
}

.result-card p {
  margin: 0 0 12px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.composer {
  padding: 16px 18px 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.95);
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.quick-chip {
  cursor: pointer;
  border-radius: 999px;
  padding: 8px 14px;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.tip-text {
  color: #64748b;
  font-size: 12px;
}

@keyframes pulse {
  0%,
  80%,
  100% {
    transform: scale(0.8);
    opacity: 0.35;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 900px) {
  .difficulty-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .mock-interview-page {
    padding: 0;
  }

  .mock-shell {
    height: 100vh;
    border-radius: 0;
  }

  .mock-header,
  .mock-main,
  .composer,
  .difficulty-panel {
    padding-left: 14px;
    padding-right: 14px;
  }

  .composer-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .tip-text {
    text-align: center;
  }

  .chat-status {
    flex-wrap: wrap;
  }
}
</style>
