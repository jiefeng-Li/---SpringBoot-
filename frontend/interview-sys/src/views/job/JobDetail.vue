<template>
  <div class="job-detail-page">
    <div class="job-detail-card" v-loading="loading">
      <template v-if="jobDetail">
        <div class="job-header">
          <div>
            <h1 class="job-title">{{ jobDetail.title || "未命名职位" }}</h1>
            <div class="job-subtitle">
              <span>{{ jobDetail.companyName || "未知公司" }}</span>
              <span class="dot">·</span>
              <span>{{ jobDetail.workCity || "工作城市待定" }}</span>
            </div>
            <div
              v-if="jobDetail.hiringManagerId"
              class="manager-entry"
              @click="openHiringManagerChat"
            >
              <el-avatar :size="28" :src="jobDetail.hiringManagerAvatar">
                {{ (jobDetail.hiringManagerName || "负").slice(0, 1) }}
              </el-avatar>
              <span class="manager-label">负责人：</span>
              <span class="manager-name">{{
                jobDetail.hiringManagerName || "未知"
              }}</span>
              <span class="manager-chat-link">聊一聊</span>
            </div>
          </div>
          <div class="salary-wrap">
            <div class="salary">
              {{ formatSalary(jobDetail.minSalary, jobDetail.maxSalary) }}
            </div>
            <div class="action-group">
              <button class="mock-btn" type="button" @click="openMockInterview">
                AI模拟面试
              </button>
              <button class="apply-btn" type="button" @click="openApplyDialog">
                立即投递
              </button>
            </div>
          </div>
        </div>

        <div class="job-meta">
          <span class="meta-item">{{
            jobDetail.jobType || "工作性质待定"
          }}</span>
          <span class="meta-item"
            >经验：{{ jobDetail.experience || "不限" }}</span
          >
          <span class="meta-item"
            >学历：{{ jobDetail.education || "不限" }}</span
          >
          <span class="meta-item"
            >招聘人数：{{ jobDetail.headcount || "若干" }}</span
          >
        </div>

        <div v-if="jobDetail.tags && jobDetail.tags.length" class="tag-list">
          <span v-for="tag in jobDetail.tags" :key="tag" class="tag">{{
            tag
          }}</span>
        </div>

        <section class="section">
          <h2>职位描述</h2>
          <p>{{ jobDetail.description || "暂无职位描述" }}</p>
        </section>

        <section class="section">
          <h2>职位要求</h2>
          <p>{{ jobDetail.requirement || "暂无职位要求" }}</p>
        </section>
      </template>

      <template v-else-if="!loading">
        <div class="empty">未获取到该职位详情</div>
      </template>
    </div>

    <el-dialog v-model="applyDialogVisible" title="投递职位" width="560px">
      <el-form
        ref="applyFormRef"
        :model="applyForm"
        :rules="applyRules"
        label-width="88px"
      >
        <el-form-item label="选择简历" prop="resumeId">
          <el-select
            v-model="applyForm.resumeId"
            placeholder="请选择简历"
            style="width: 100%"
          >
            <el-option
              v-for="item in resumeOptions"
              :key="item.id"
              :label="`${item.name}（${item.city}）${item.isDefault ? ' [默认]' : ''}`"
              :value="item.id"
            />
          </el-select>
          <div v-if="resumeLoading" class="resume-hint">简历加载中...</div>
          <div v-else-if="!resumeNoMore" class="resume-hint">
            <el-button link type="primary" @click="loadResumeOptions"
              >加载更多简历</el-button
            >
          </div>
        </el-form-item>

        <el-form-item label="求职信" prop="coverLetter">
          <el-input
            v-model="applyForm.coverLetter"
            type="textarea"
            :rows="5"
            maxlength="1024"
            show-word-limit
            placeholder="可填写你的优势、到岗时间等信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="applySubmitting"
          @click="submitApplication"
          >确认投递</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores";
import { getJobPositionById, jobView } from "@/api/job";
import { getResumesPageByUserId } from "@/api/resume";
import { addJobApplication } from "@/api/application";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const jobDetail = ref(null);
const applyDialogVisible = ref(false);
const applyFormRef = ref(null);
const applySubmitting = ref(false);
const resumeLoading = ref(false);
const resumeNoMore = ref(false);
const resumePageNum = ref(1);
const resumePageSize = ref(10);
const resumeOptions = ref([]);
const applyForm = reactive({
  resumeId: null,
  coverLetter: "",
});

const applyRules = {
  resumeId: [{ required: true, message: "请选择投递简历", trigger: "change" }],
  coverLetter: [
    { max: 1024, message: "求职信不能超过1024字", trigger: "blur" },
  ],
};

const formatSalary = (minSalary, maxSalary) => {
  if (minSalary == null && maxSalary == null) {
    return "薪资面议";
  }
  if (minSalary != null && maxSalary != null) {
    return `${minSalary}-${maxSalary}元/月`;
  }
  if (minSalary != null) {
    return `${minSalary}元/月起`;
  }
  return `最高${maxSalary}元/月`;
};

const openHiringManagerChat = () => {
  const managerId = Number(jobDetail.value?.hiringManagerId || 0);
  if (!managerId) {
    ElMessage.warning("暂无负责人信息");
    return;
  }
  router.push({
    path: "/personal/message",
    query: {
      targetUserId: String(managerId),
      targetName: jobDetail.value?.hiringManagerName || "",
      targetAvatar: jobDetail.value?.hiringManagerAvatar || "",
    },
  });
};

const fetchJobDetail = async () => {
  const id = route.params.id;
  if (!id) {
    return;
  }
  loading.value = true;
  try {
    const res = await getJobPositionById(id);
    jobView(id); // 记录职位浏览量，异步调用，不等待结果
    jobDetail.value = res?.data.data || null;
  } catch (error) {
    console.error("获取职位详情失败:", error);
    jobDetail.value = null;
  } finally {
    loading.value = false;
  }
};

const extractPageData = (res) => {
  const payload = res?.data?.data ?? res?.data ?? {};
  return {
    list: payload?.list || [],
    pages: Number(payload?.pages || 0),
    pageNum: Number(payload?.pageNum || 1),
  };
};

const resetResumeState = () => {
  resumeOptions.value = [];
  resumePageNum.value = 1;
  resumeNoMore.value = false;
};

const loadResumeOptions = async () => {
  if (resumeLoading.value || resumeNoMore.value) return;
  if (!userStore.userId) {
    ElMessage.warning("未获取到用户信息，请重新登录");
    resumeNoMore.value = true;
    return;
  }

  resumeLoading.value = true;
  try {
    const res = await getResumesPageByUserId({
      userId: userStore.userId,
      pageNum: resumePageNum.value,
      pageSize: resumePageSize.value,
    });

    const pageData = extractPageData(res);
    const list = pageData.list
      .map((item) => ({
        id: item.id ?? item.resumeId,
        name: item.name || "未命名简历",
        city: item.city || "未知城市",
        isDefault: item.isDefault === 1,
      }))
      .filter((item) => !!item.id);

    resumeOptions.value.push(...list);

    if (pageData.pageNum >= pageData.pages || list.length === 0) {
      resumeNoMore.value = true;
    } else {
      resumePageNum.value += 1;
    }

    if (!applyForm.resumeId && resumeOptions.value.length) {
      const defaultResume = resumeOptions.value.find((item) => item.isDefault);
      applyForm.resumeId = (defaultResume || resumeOptions.value[0]).id;
    }
  } catch (error) {
    resumeNoMore.value = true;
    ElMessage.error("加载简历失败，请稍后重试");
  } finally {
    resumeLoading.value = false;
  }
};

const openApplyDialog = async () => {
  if (!jobDetail.value) {
    ElMessage.warning("职位信息异常，暂无法投递");
    return;
  }

  applyDialogVisible.value = true;
  applyForm.resumeId = null;
  applyForm.coverLetter = "";
  resetResumeState();
  await loadResumeOptions();
};

const openMockInterview = () => {
  if (!jobDetail.value) {
    ElMessage.warning("职位信息异常，暂无法进入模拟面试");
    return;
  }

  router.push({
    path: "/meeting/mock-interview",
    query: {
      jobId: String(route.params.id || ""),
    },
  });
};

const submitApplication = async () => {
  await applyFormRef.value?.validate();

  if (!jobDetail.value?.companyId) {
    ElMessage.warning("职位缺少公司信息，无法投递");
    return;
  }

  applySubmitting.value = true;
  try {
    await addJobApplication({
      companyId: jobDetail.value.companyId,
      jobPositionId: Number(route.params.id),
      userId: userStore.userId,
      resumeId: applyForm.resumeId,
      coverLetter: applyForm.coverLetter?.trim() || "",
    });
    applyDialogVisible.value = false;
  } catch (error) {
    ElMessage.error("投递失败，请稍后重试");
  } finally {
    applySubmitting.value = false;
  }
};

onMounted(() => {
  fetchJobDetail();
});
</script>

<style scoped>
.job-detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 16px;
}

.job-detail-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 8px 24px rgb(0 0 0 / 6%);
}

.job-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 20px;
}

.job-title {
  margin: 0;
  font-size: 34px;
  line-height: 1.25;
  color: #222;
}

.job-subtitle {
  margin-top: 12px;
  color: #666;
  font-size: 14px;
}

.manager-entry {
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f7fbff;
  border: 1px solid #dbeafe;
  cursor: pointer;
  user-select: none;
}

.manager-entry:hover {
  background: #edf5ff;
}

.manager-label {
  color: #4b5563;
  font-size: 13px;
}

.manager-name {
  color: #111827;
  font-size: 13px;
  font-weight: 600;
}

.manager-chat-link {
  color: #1677ff;
  font-size: 13px;
}

.dot {
  margin: 0 8px;
}

.salary-wrap {
  text-align: right;
  min-width: 220px;
}

.action-group {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.salary {
  font-size: 28px;
  font-weight: 700;
  color: #e53935;
  margin-bottom: 12px;
}

.apply-btn {
  border: none;
  background: #1677ff;
  color: #fff;
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 16px;
  cursor: pointer;
}

.mock-btn {
  border: 1px solid #1677ff;
  background: #fff;
  color: #1677ff;
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 16px;
  cursor: pointer;
}

.mock-btn:hover {
  background: #f0f7ff;
}

.apply-btn:hover {
  background: #0958d9;
}

.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 18px 0;
}

.meta-item {
  background: #f6f8fa;
  color: #444;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 14px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.tag {
  background: #eef5ff;
  color: #1d4ed8;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 13px;
}

.section {
  margin-top: 18px;
}

.section h2 {
  font-size: 20px;
  margin-bottom: 10px;
  color: #222;
}

.section p {
  margin: 0;
  color: #444;
  line-height: 1.8;
  white-space: pre-line;
}

.empty {
  text-align: center;
  color: #888;
  padding: 50px 0;
}

.resume-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #888;
}

@media (max-width: 768px) {
  .job-header {
    flex-direction: column;
  }

  .salary-wrap {
    text-align: left;
  }

  .job-title {
    font-size: 28px;
  }
}
</style>
