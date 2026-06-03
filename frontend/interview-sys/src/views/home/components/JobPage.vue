<template>
  <div class="job-page-container">
    <div class="search-box">
      <el-input
        v-model="keyword"
        :trigger-on-focus="false"
        clearable
        placeholder="搜索职位、公司等"
      >
        <template #suffix>
          <el-icon
            ><Search @click="queryAndJump" style="cursor: pointer"
          /></el-icon>
        </template>
      </el-input>
    </div>
    <div class="job-conditions-form-box">
      <el-collapse expand-icon-position="left" style="width: 100%">
        <el-collapse-item title="筛选" name="1">
          <el-form :model="jobConditions" label-width="auto">
            <el-form-item label="职位类型">
              <el-radio-group
                v-model="jobConditions.jobType"
                text-color="#fff"
                fill="#409eff"
              >
                <el-radio-button>不限</el-radio-button>
                <el-radio-button value="0">全职</el-radio-button>
                <el-radio-button value="1">兼职</el-radio-button>
                <el-radio-button value="2">实习</el-radio-button>
                <el-radio-button value="3">远程</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="薪资待遇">
              <el-row style="margin-bottom: 18px">
                <el-radio-group
                  v-model="jobConditions.salary"
                  text-color="#fff"
                  fill="#409eff"
                  @change="handleSalaryChange"
                >
                  <el-radio-button value="不限">不限</el-radio-button>
                  <el-radio-button value="0">3K以下</el-radio-button>
                  <el-radio-button value="1">3-5K</el-radio-button>
                  <el-radio-button value="2">5-10K</el-radio-button>
                  <el-radio-button value="3">10-20K</el-radio-button>
                  <el-radio-button value="4">20-50K</el-radio-button>
                  <el-radio-button value="5">50K以上</el-radio-button>
                </el-radio-group>
              </el-row>
              <el-row>
                <el-col :span="11"
                  ><el-input-number
                    style="width: 100%"
                    v-model="jobConditions.salaryMin"
                    :min="1"
                  >
                  </el-input-number
                ></el-col>
                <el-col class="text-center" :span="1" style="margin: 0 0.5rem"
                  >-</el-col
                >
                <el-col :span="11">
                  <el-input-number
                    style="width: 100%"
                    v-model="jobConditions.salaryMax"
                    :min="1"
                  >
                  </el-input-number
                ></el-col>
              </el-row>
            </el-form-item>
            <!-- <el-form-item label=""> </el-form-item> -->
            <el-form-item label="工作经验">
              <el-radio-group
                v-model="jobConditions.experience"
                text-color="#fff"
                fill="#409eff"
              >
                <el-radio-button>不限</el-radio-button>
                <el-radio-button value="在校生">在校生</el-radio-button>
                <el-radio-button value="应届生">应届生</el-radio-button>
                <el-radio-button value="经验不限">经验不限</el-radio-button>
                <el-radio-button value="一年以内">一年以内</el-radio-button>
                <el-radio-button value="1-3年">1-3年</el-radio-button>
                <el-radio-button value="3-5年">3-5年</el-radio-button>
                <el-radio-button value="5-10年">5-10年</el-radio-button>
                <el-radio-button value="10年以上">10年以上</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="学历要求">
              <el-radio-group
                v-model="jobConditions.education"
                text-color="#fff"
                fill="#409eff"
              >
                <el-radio-button>不限</el-radio-button>
                <el-radio-button value="初中以以下">初中以以下</el-radio-button>
                <el-radio-button value="中专/中技">中专/中技</el-radio-button>
                <el-radio-button value="高中">高中</el-radio-button>
                <el-radio-button value="大专">大专</el-radio-button>
                <el-radio-button value="本科">本科</el-radio-button>
                <el-radio-button value="硕士">硕士</el-radio-button>
                <el-radio-button value="博士">博士</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="公司规模">
              <el-radio-group
                v-model="jobConditions.scale"
                text-color="#fff"
                fill="#409eff"
              >
                <el-radio-button>不限</el-radio-button>
                <el-radio-button value="0-20人">0-20人</el-radio-button>
                <el-radio-button value="20-99人">20-99人</el-radio-button>
                <el-radio-button value="100-499人">100-499人</el-radio-button>
                <el-radio-button value="500-999人">500-999人</el-radio-button>
                <el-radio-button value="1000-9999人"
                  >1000-9999人</el-radio-button
                >
                <el-radio-button value="10000人以上"
                  >10000人以上</el-radio-button
                >
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
    </div>
    <div class="job-list-container">
      <el-container>
        <el-aside>
          <el-scrollbar @end-reached="loadMore">
            <div
              v-for="job in jobList"
              :key="job.id"
              class="job-item"
              :class="{ active: selectedJob && selectedJob.id === job.id }"
              @click="handleJobDetail(job)"
            >
              <div class="job-title">{{ job.title }}</div>
              <div class="job-salary">
                {{ formatSalary(job.minSalary, job.maxSalary) }}
              </div>
              <div class="job-company">{{ job.companyName }}</div>
              <div class="job-hr">{{ job.hiringManagerName }}</div>
              <div class="job-tags">
                <el-tag
                  v-for="(tag, index) in job.tags?.slice(0, 3)"
                  :key="index"
                  size="small"
                  type="info"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </el-scrollbar>
        </el-aside>
        <el-main>
          <el-card class="job-detail" shadow="always" v-loading="loading">
            <template v-if="selectedJob">
              <div class="detail-header">
                <h2 class="job-title">{{ selectedJob.title }}</h2>
                <div class="job-salary">
                  {{
                    formatSalary(selectedJob.minSalary, selectedJob.maxSalary)
                  }}
                </div>
                <el-button type="primary" @click="handleApply"
                  >立即投递</el-button
                >
              </div>
              <div class="detail-tags">
                <el-tag
                  v-for="(tag, index) in selectedJob.tags"
                  :key="index"
                  size="small"
                  type="info"
                >
                  {{ tag }}
                </el-tag>
              </div>
              <el-divider />
              <div class="detail-section">
                <h3>职位描述</h3>
                <div class="detail-content">{{ selectedJob.description }}</div>
              </div>
              <div class="detail-section">
                <h3>职位要求</h3>
                <div class="detail-content">{{ selectedJob.requirement }}</div>
              </div>
              <el-divider />
              <div class="detail-info">
                <div class="info-item">
                  <span class="info-label">工作城市：</span>
                  <span class="info-value">{{ selectedJob.workCity }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">工作性质：</span>
                  <span class="info-value">{{ selectedJob.jobType }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">经验要求：</span>
                  <span class="info-value">{{ selectedJob.experience }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">学历要求：</span>
                  <span class="info-value">{{ selectedJob.education }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">招聘人数：</span>
                  <span class="info-value">{{ selectedJob.headcount }}人</span>
                </div>
                <div class="info-item">
                  <span class="info-label">职位状态：</span>
                  <span class="info-value">{{ selectedJob.status }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">发布时间：</span>
                  <span class="info-value">{{ selectedJob.publishTime }}</span>
                  <!-- formatDate(selectedJob.publishTime) -->
                </div>
                <div class="info-item">
                  <span class="info-label">浏览量：</span>
                  <span class="info-value">{{ selectedJob.viewCount }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">投递量：</span>
                  <span class="info-value">{{ selectedJob.applyCount }}</span>
                </div>
              </div>
              <el-divider />
              <div class="company-info">
                <h3>公司信息</h3>
                <div class="info-item">
                  <span class="info-label">公司名称：</span>
                  <span class="info-value">{{ selectedJob.companyName }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">招聘负责人：</span>
                  <span class="info-value">{{
                    selectedJob.hiringManagerName
                  }}</span>
                </div>
              </div>
            </template>
            <div v-else class="empty-detail">请选择一个职位查看详情</div>
          </el-card>
        </el-main>
      </el-container>
    </div>
    <!-- <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 30, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div> -->

    <el-backtop :right="100" :bottom="100" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from "vue";
import { Search } from "@element-plus/icons-vue";
import { getJobPositionList } from "@/api/job";
import { useUserStore } from "@/stores";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
// import { formatDate } from "@/utils/format";
const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const jobDetail = ref({
  index: 0,
});

const jobConditions = ref({
  jobType: undefined,
  salary: undefined,
  salaryMin: undefined,
  salaryMax: undefined,
  experience: undefined,
  education: undefined,
  scale: undefined,
});

// 职位列表数据
const jobList = ref([]);
// 当前选中的职位
const selectedJob = ref(null);
// 分页数据
const pagination = ref({
  total: 0,
  pageSize: 10,
  pageNum: 1,
  pages: 0,
});
// 加载状态
const loading = ref(false);

const handleSalaryChange = (value) => {
  switch (value) {
    case "不限":
      jobConditions.value.salaryMin = undefined;
      jobConditions.value.salaryMax = undefined;
      break;
    case "0": // 3K以下
      jobConditions.value.salaryMin = undefined;
      jobConditions.value.salaryMax = 3000;
      break;
    case "1": // 3-5K
      jobConditions.value.salaryMin = 3000;
      jobConditions.value.salaryMax = 5000;
      break;
    case "2": // 5-10K
      jobConditions.value.salaryMin = 5000;
      jobConditions.value.salaryMax = 10000;
      break;
    case "3": // 10-20K
      jobConditions.value.salaryMin = 10000;
      jobConditions.value.salaryMax = 20000;
      break;
    case "4": // 20-50K
      jobConditions.value.salaryMin = 20000;
      jobConditions.value.salaryMax = 50000;
      break;
    case "5": // 50K以上
      jobConditions.value.salaryMin = 50000;
      jobConditions.value.salaryMax = undefined;
      break;
    default:
      jobConditions.value.salaryMin = undefined;
      jobConditions.value.salaryMax = undefined;
  }
};

const keyword = ref("");

const queryAndJump = async () => {
  pagination.value.pageNum = 1;
  await router.replace({
    path: "/job",
    query: keyword.value ? { keyword: keyword.value } : {},
  });
};

const handleJobDetail = (item) => {
  console.log(item);
  // jobDetail.value.index = item;
  selectedJob.value = item;
};

// 获取职位列表
const fetchJobs = async (isAppend = false) => {
  loading.value = true;
  try {
    const routeKeyword = route.query.keyword;
    const searchKeyword =
      keyword.value || (routeKeyword ? String(routeKeyword) : "");
    // 构建请求参数
    const params = {
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
      status: 1, // 招聘中
    };

    // 添加筛选条件
    if (jobConditions.value.jobType) {
      params.jobType = jobConditions.value.jobType;
    }
    if (jobConditions.value.salaryMin) {
      params.minSalary = jobConditions.value.salaryMin;
    }
    if (jobConditions.value.salaryMax) {
      params.maxSalary = jobConditions.value.salaryMax;
    }
    if (jobConditions.value.experience) {
      params.experience = jobConditions.value.experience;
    }
    if (jobConditions.value.education) {
      params.education = jobConditions.value.education;
    }
    if (jobConditions.value.scale) {
      params.scale = jobConditions.value.scale;
    }
    if (searchKeyword) {
      params.keyword = searchKeyword;
    }

    // 调用API获取数据
    const res = await getJobPositionList(params);
    if (res.data.code === 200) {
      const newList = res.data.data.list || [];
      pagination.value.total = res.data.data.total || 0;
      pagination.value.pages = res.data.data.pages || 0;

      if (isAppend) {
        // 追加数据
        jobList.value = [...jobList.value, ...newList];
      } else {
        // 替换数据
        jobList.value = newList;

        // 如果有职位数据且当前没有选中的职位，默认选中第一个
        if (jobList.value.length > 0) {
          selectedJob.value = jobList.value[0];
        } else {
          selectedJob.value = null;
        }
      }
    } else {
      ElMessage.error(res.data.message || "获取职位列表失败");
    }
  } catch (error) {
    console.error("获取职位列表失败:", error);
    ElMessage.error("获取职位列表失败");
  } finally {
    loading.value = false;
  }
};

// 每页显示数量变化
const handleSizeChange = (val) => {
  pagination.value.pageSize = val;
  fetchJobs();
};

// 当前页码变化
const handleCurrentChange = (val) => {
  pagination.value.pageNum = val;
  fetchJobs();
};

const loadMore = (direction) => {
  if (direction === "bottom" && jobList.value.length < pagination.value.total) {
    pagination.value.pageNum++;
    fetchJobs(true); // 传入 true 表示追加数据
  }
};

// 格式化薪资
const formatSalary = (minSalary, maxSalary) => {
  if (!minSalary && !maxSalary) return "薪资面议";
  if (!minSalary) return `${maxSalary}K以下`;
  if (!maxSalary) return `${minSalary}K以上`;
  return `${minSalary}-${maxSalary}K`;
};

// 处理投递点击事件
const handleApply = () => {
  if (!userStore.isloginned) {
    ElMessage.warning("请先登录");
    router.push("/login");
  } else {
    router.push(`/job/${selectedJob.value.id}`); // 跳转到投递页面，传递职位ID
  }
};

watch(
  () => route.query.keyword,
  (newKeyword) => {
    keyword.value = newKeyword ? String(newKeyword) : "";
    pagination.value.pageNum = 1;
    fetchJobs(false);
  },
  { immediate: true },
);

// 监听筛选条件变化
// watch(
//   () => [
//     jobConditions.value.jobType,
//     jobConditions.value.experience,
//     jobConditions.value.education,
//     jobConditions.value.scale,
//     jobConditions.value.salaryMin,
//     jobConditions.value.salaryMax,
//   ],
//   () => {
//     pagination.value.pageNum = 1;
//     fetchJobs(false); // 筛选条件变化时，不追加数据，而是替换
//   },
//   { deep: true },
// );
</script>

<style lang="scss" scoped>
.job-page-container {
  height: 100%;
  width: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  .search-box {
    width: 80%;
    height: 5em;
    display: flex;
    justify-content: center;
    align-items: center;
    margin: 0 auto;
  }
  .job-conditions-form-box {
    width: 80%;
    display: flex;
    margin: 0 auto;
    //flex-shrink: 0;
    :deep(.el-collapse-item__content) {
      padding-bottom: 0;
    }
  }
  .job-list-container {
    width: 100%;
    // background-color: #3b3b3b;
    margin-top: 18px;
    flex: 1;
    overflow: auto;
    display: flex;
    .scrollbar-demo-item {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 50px;
      margin: 10px;
      text-align: center;
      border-radius: 4px;
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
    }
    .job-detail {
      height: 100%;
      width: 100%;
    }
  }
  .job-item {
    padding: 12px;
    margin-bottom: 10px;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background-color: var(--el-color-primary-light-9);
    }

    &.active {
      background-color: var(--el-color-primary-light-8);
      border-left: 3px solid var(--el-color-primary);
    }

    .job-title {
      font-size: 16px;
      font-weight: bold;
      margin-bottom: 8px;
    }

    .job-salary {
      color: #ff5722;
      font-weight: bold;
      margin-bottom: 8px;
    }

    .job-company,
    .job-hr {
      font-size: 14px;
      color: #606266;
      margin-bottom: 8px;
    }

    .job-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }
  }

  .job-detail {
    height: 100%;
    width: 100%;

    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .job-title {
        margin: 0;
        font-size: 20px;
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 16px;
      }

      .job-salary {
        color: #ff5722;
        font-size: 18px;
        font-weight: bold;
      }
    }

    .detail-tags {
      margin-bottom: 16px;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .detail-section {
      margin-bottom: 20px;

      h3 {
        margin: 0 0 10px;
        font-size: 16px;
      }

      .detail-content {
        line-height: 1.8;
        color: #606266;
        white-space: pre-line;
      }
    }

    .detail-info {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;

      .info-item {
        display: flex;

        .info-label {
          color: #909399;
          min-width: 80px;
        }

        .info-value {
          color: #303133;
        }
      }
    }

    .company-info {
      margin-top: 20px;

      h3 {
        margin: 0 0 12px;
        font-size: 16px;
      }
    }

    .empty-detail {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100%;
      color: #909399;
    }
  }
}
</style>
