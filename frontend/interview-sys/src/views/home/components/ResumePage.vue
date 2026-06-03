<template>
  <div class="resume-page-container">
    <el-container>
      <el-header>
        <el-image
          style="width: 100px; height: 100px"
          :src="userStore.avatarUrl || ''"
        />
        <div class="states-data">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="6" class="text-center mb-4">
              <el-statistic title="简历数量" :value="outputValue" />
            </el-col>
            <!-- <el-col :xs="24" :sm="12" :md="6" class="text-center mb-4">
              <el-statistic :value="138">
                <template #title>
                  <div style="display: inline-flex; align-items: center">
                    Ratio of men to women
                    <el-icon style="margin-left: 4px" :size="12">
                      <Male />
                    </el-icon>
                  </div>
                </template>
                <template #suffix>/100</template>
              </el-statistic>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6" class="text-center mb-4">
              <el-statistic title="Total Transactions" :value="outputValue" />
            </el-col>
            <el-col :xs="24" :sm="12" :md="6" class="text-center mb-4">
              <el-statistic title="Feedback number" :value="562">
                <template #suffix>
                  <el-icon style="vertical-align: -0.125em">
                    <ChatLineRound />
                  </el-icon>
                </template>
              </el-statistic>
            </el-col> -->
          </el-row>
        </div>
      </el-header>
      <el-main>
        <el-container style="height: 100%">
          <el-main style="padding: 0">
            <div
              style="
                height: 4em;
                width: 100%;
                display: flex;
                justify-content: flex-end;
                align-items: center;
                gap: 10px;
              "
            >
              <el-button type="primary" :icon="Edit" @click="goToCreateResume"
                >新建</el-button
              >
            </div>
            <div style="height: calc(100% - 120px)">
              <el-scrollbar @end-reached="handleEndReached">
                <transition-group name="list" tag="div">
                  <div
                    v-for="item in resumes"
                    :key="item._key"
                    class="scrollbar-resume-item"
                  >
                    <div class="resuem-info">
                      <el-avatar :size="42" :src="item.avatar || ''">
                        {{ item.city?.[0] || "简" }}
                      </el-avatar>
                      <div class="resume-main">
                        <div class="resume-title-row">
                          <span class="resume-title">{{
                            item.name || "未命名简历"
                          }}</span>
                          <el-tag
                            v-if="item.isDefault === 1"
                            size="small"
                            type="success"
                            >默认</el-tag
                          >
                        </div>
                        <div class="resume-meta">
                          <span>{{ item.city || "未知城市" }}</span>
                          <span>{{ item.address || "暂无地址" }}</span>
                          <span>模板 {{ item.templateId ?? "-" }}</span>
                          <span>{{ formatDate(item.createTime) }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="buttons">
                      <el-tooltip
                        class="box-item"
                        effect="light"
                        content="设为默认"
                        placement="top"
                        :disabled="item.isDefault === 1"
                      >
                        <el-button
                          type="info"
                          :icon="Star"
                          circle
                          @click="setDefalutResume(item)"
                          v-if="item.isDefault !== 1"
                        />
                        <el-button
                          type="success"
                          :icon="Check"
                          circle
                          v-if="item.isDefault === 1"
                        />
                      </el-tooltip>

                      <el-button
                        type="primary"
                        :icon="Edit"
                        circle
                        @click="goToEdit(item)"
                      />
                      <el-popconfirm
                        class="box-item"
                        title="是否确认删除？"
                        placement="top"
                        @confirm="removeResume(item)"
                      >
                        <template #reference>
                          <el-button type="danger" :icon="Delete" circle />
                        </template>
                      </el-popconfirm>
                    </div>
                  </div>
                </transition-group>
                <div class="load-state" v-if="loading">加载中...</div>
                <div
                  class="load-state"
                  v-else-if="noMore && resumes.length > 0"
                >
                  没有更多了
                </div>
                <el-empty
                  v-else-if="!loading && resumes.length === 0"
                  description="暂无简历"
                  :image-size="80"
                />
              </el-scrollbar>
            </div>
          </el-main>
          <el-aside></el-aside>
        </el-container>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { Edit, Upload, Delete, Star, Check } from "@element-plus/icons-vue";
import { onMounted, onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores";
import {
  getResumesPageByUserId,
  setDefaultResume,
  deleteResume as deleteResumeById,
} from "@/api/resume";

const resumes = ref([]);
const outputValue = ref(0);
const loading = ref(false);
const noMore = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const router = useRouter();
const userStore = useUserStore();
const totalCnt = ref(0);

const goToCreateResume = () => {
  router.push("/resume/create");
};

const goToEdit = (resume) => {
  const id = resume.id ?? resume.resumeId;
  if (!id) {
    ElMessage.warning("该简历缺少ID，无法编辑");
    return;
  }
  router.push({ path: "/resume/create", query: { resumeId: id } });
};

const formatDate = (dateValue) => {
  if (!dateValue) return "-";
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return String(dateValue);
  return date.toLocaleDateString("zh-CN");
};

const extractPageData = (res) => {
  const payload = res?.data?.data ?? res?.data ?? {};
  return {
    list: payload?.list || [],
    total: Number(payload?.total || 0),
    pages: Number(payload?.pages || 0),
    pageNum: Number(payload?.pageNum || 1),
  };
};

const loadResumes = async () => {
  if (loading.value || noMore.value) return;
  if (!userStore.userId) {
    ElMessage.warning("未获取到用户信息，请重新登录");
    noMore.value = true;
    return;
  }

  loading.value = true;
  try {
    const res = await getResumesPageByUserId({
      userId: userStore.userId,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });

    const pageData = extractPageData(res);
    const newItems = pageData.list.map((item, index) => ({
      ...item,
      _key:
        item.id ??
        item.resumeId ??
        `${pageData.pageNum}-${index}-${item.templateId ?? "tpl"}`,
    }));

    resumes.value.push(...newItems);
    outputValue.value = pageData.total;

    if (pageData.pageNum >= pageData.pages || newItems.length === 0) {
      noMore.value = true;
    } else {
      pageNum.value += 1;
    }
  } catch (error) {
    noMore.value = true;
  } finally {
    loading.value = false;
  }
};

const handleEndReached = (direction) => {
  if (direction !== "bottom") return;
  loadResumes();
};

const setDefalutResume = async (resume) => {
  const targetId = resume.id;
  if (!targetId) {
    ElMessage.warning("该简历缺少ID，无法设置默认");
    return;
  }
  await setDefaultResume(targetId);

  resumes.value = resumes.value.map((item) => ({
    ...item,
    isDefault: item._key === resume._key ? 1 : 0,
  }));
  ElMessage.success("已设为默认简历");
};

const removeResume = async (resume) => {
  const targetId = resume.id ?? resume.resumeId;
  if (!targetId) {
    ElMessage.warning("该简历缺少ID，无法删除");
    return;
  }

  await deleteResumeById(targetId);
  resumes.value = resumes.value.filter((item) => item._key !== resume._key);
  outputValue.value = Math.max(outputValue.value - 1, 0);
  if (!noMore.value && resumes.value.length < pageSize.value) {
    loadResumes();
  }

  ElMessage.success("删除成功");
};

onMounted(() => {
  document.body.style.overflow = "hidden";
  loadResumes();
});

onBeforeUnmount(() => {
  document.body.style.overflow = "";
});
</script>

<style lang="scss" scoped>
.resume-page-container {
  width: 100%;
  // height: 100%;
  height: 100vh;
  display: flex;
  overflow: hidden;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
  .el-container {
    .el-header {
      background-color: #b3c0d1;
      width: 100%;
      height: 120px;
      display: flex;
      align-items: center;
      .states-data {
        width: 75%;
      }
    }
  }
  .scrollbar-resume-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    min-height: 76px;
    margin: 10px;
    border-radius: 4px;
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    padding: 10px 14px;
  }
  .resuem-info {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }
  .resume-main {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
  }
  .resume-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .resume-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
  .resume-meta {
    display: flex;
    align-items: center;
    gap: 14px;
    color: var(--el-text-color-regular);
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .buttons {
    display: flex;
    gap: 5px;
    //禁用选中
  }
  .list-move {
    transition: transform 0.3s ease;
  }
  .list-enter-active,
  .list-leave-active {
    transition: all 0.3s;
  }
  .list-enter-from,
  .list-leave-to {
    opacity: 0;
    transform: translateX(30px);
  }
  .tooltip-base-box .box-item {
    width: 110px;
    margin-top: 10px;
  }
  .load-state {
    text-align: center;
    color: var(--el-text-color-secondary);
    padding: 10px 0 16px;
    font-size: 13px;
  }
}
</style>
