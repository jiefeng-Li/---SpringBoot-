<template>
  <div class="home-page-container">
    <div class="search-box">
      <el-autocomplete
        v-model="keyword"
        :fetch-suggestions="querySearchAsync"
        :trigger-on-focus="false"
        clearable
        placeholder="搜索职位、公司等"
      >
        <template #suffix>
          <el-icon
            ><Search @click="queryAndJump" style="cursor: pointer"
          /></el-icon>
        </template>
      </el-autocomplete>
    </div>
    <div class="banner-container">
      <div class="banner-menu">
        <el-scrollbar>
          <el-menu>
            <el-menu-item
              v-for="(item, index) in bannerCategories"
              :index="item.name"
              :key="item.name"
              @mouseenter="handleMouseEnter(index)"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <div class="menu-item-content">
                <span class="menu-title">{{ item.name }}</span>
                <span class="menu-subtitle">{{ item.subtitle }}</span>
              </div>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </div>
      <div class="banner-main">
        <div class="banner-hero-layout">
          <div class="banner-hero-copy">
            <!-- <p class="feature-kicker">{{ activeCategory.kicker }}</p>
            <h2>{{ activeCategory.title }}</h2>
            <p class="feature-description">{{ activeCategory.description }}</p> -->
            <!-- <div class="feature-stat-row">
              <div
                v-for="stat in activeCategory.stats"
                :key="stat.label"
                class="feature-stat"
              >
                <strong>{{ stat.value }}</strong>
                <span>{{ stat.label }}</span>
              </div>
            </div> -->
            <div class="feature-tag-list">
              <el-tag
                v-for="tag in activeCategory.tags"
                :key="tag"
                class="feature-tag"
                effect="light"
                round
                @click="handleTagClick(tag)"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="popular-job-container">
      <h2 style="text-align: center">热门职位</h2>
      <div class="job-card-box">
        <div
          v-for="item in jobList"
          :key="item.id"
          class="job-card"
          @click="goToJobDetail(item.id)"
        >
          <div class="job-card-header">
            <h3>{{ item.title || "未命名职位" }}</h3>
            <p class="salary">
              {{
                item.minSalary && item.maxSalary
                  ? `${item.minSalary}-${item.maxSalary} 元`
                  : "薪资面议"
              }}
            </p>
          </div>
          <p class="job-company">{{ item.companyName || "未知公司" }}</p>
          <div class="job-meta">
            <span>{{ item.workCity || "城市待定" }}</span>
            <span>{{ item.experience || "经验不限" }}</span>
          </div>
          <div class="job-manager">
            <el-avatar :size="24" :src="item.hiringManagerAvatar">
              {{ (item.hiringManagerName || "招").slice(0, 1) }}
            </el-avatar>
            <span>负责人：{{ item.hiringManagerName || "未设置" }}</span>
          </div>
        </div>
      </div>
      <div style="display: flex; justify-content: center">
        <el-button
          type="primary"
          round
          class="more-info-btn"
          @click="router.push('/job')"
          >更多职位</el-button
        >
      </div>
    </div>
    <div class="popular-company-container">
      <h2 style="text-align: center">热门企业</h2>
      <div class="company-card-box">
        <div
          v-for="item in companyList"
          :key="item.companyId"
          class="company-card"
          @click="goToCompanyDetail(item.companyId)"
        >
          <div class="company-head">
            <el-avatar :size="52" :src="item.logoUrl">
              {{ (item.companyName || "企").slice(0, 1) }}
            </el-avatar>
            <div class="company-title">
              <h3>{{ item.companyName || "未命名公司" }}</h3>
              <p>{{ item.city || "城市待定" }}</p>
            </div>
          </div>
          <div class="company-tags">
            <el-tag size="small" type="primary">{{
              item.industry || "行业待完善"
            }}</el-tag>
            <el-tag size="small" type="success">{{
              item.scale || "规模待完善"
            }}</el-tag>
          </div>
          <p class="company-intro">
            {{ item.introduction || "该公司暂未完善简介" }}
          </p>
        </div>
      </div>
      <div style="display: flex; justify-content: center">
        <el-button
          type="primary"
          round
          class="more-info-btn"
          @click="router.push('/company')"
          >更多企业</el-button
        >
      </div>
    </div>
    <el-backtop :right="100" :bottom="100" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import {
  Search,
  Lightning,
  SetUp,
  Brush,
  DataLine,
  Cpu,
  Operation,
} from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { getJobPositionList, getRecommendedJobPositionList } from "@/api/job";
import { getCompanyList } from "@/api/company";
import { useUserStore } from "@/stores";

const router = useRouter();
const userStore = useUserStore();
const jobPositionZIndex = ref(-1);
const activeCategoryIndex = ref(0);
const jobList = ref([]);
const companyList = ref([]);
const homeJobPageSize = 8;
const homeCompanyPageSize = 6;

const bannerCategories = [
  {
    name: "技术",
    subtitle: "后端 / 前端 / 测试 / 架构",
    icon: Cpu,
    kicker: "热门技术岗位",
    title: "技术驱动，机会密集",
    description:
      "聚焦 Java、前端、后端、测试等热门岗位，快速直达你关心的技能方向。",
    stats: [
      { label: "热门标签", value: "12+" },
      { label: "活跃岗位", value: "3k+" },
      { label: "平均薪资", value: "20K+" },
    ],
    tags: [
      "Java",
      "Spring Boot",
      "Spring Cloud",
      "Vue",
      "React",
      "MySQL",
      "Redis",
      "Docker",
      "Kubernetes",
      "微服务",
      "Go",
      "Python",
    ],
  },
  {
    name: "产品",
    subtitle: "产品经理 / 运营 / 策划",
    icon: Operation,
    kicker: "产品与运营",
    title: "从需求到增长，岗位更全面",
    description:
      "适合产品、运营、增长方向，覆盖从业务分析到用户增长的多类岗位。",
    stats: [
      { label: "热门标签", value: "9+" },
      { label: "活跃岗位", value: "1.5k+" },
      { label: "适合人群", value: "应届 / 经验" },
    ],
    tags: [
      "产品经理",
      "用户运营",
      "活动策划",
      "增长",
      "数据分析",
      "项目管理",
      "需求分析",
      "商业分析",
      "A/B测试",
      "转化率优化",
    ],
  },
  {
    name: "设计",
    subtitle: "UI / UX / 视觉 / 动效",
    icon: Brush,
    kicker: "设计创意",
    title: "视觉表达与用户体验并重",
    description: "为 UI、UX、视觉设计、交互设计相关岗位提供快速入口。",
    stats: [
      { label: "热门标签", value: "8+" },
      { label: "活跃岗位", value: "900+" },
      { label: "推荐城市", value: "一线 / 新一线" },
    ],
    tags: [
      "UI设计",
      "UX设计",
      "交互设计",
      "视觉设计",
      "动效",
      "Figma",
      "Sketch",
      "Axure",
      "Photoshop",
      "Illustrator",
    ],
  },
  {
    name: "数据",
    subtitle: "算法 / 分析 / BI / 数仓",
    icon: DataLine,
    kicker: "数据智能",
    title: "数据价值正在放大",
    description: "围绕数据分析、算法、BI、数仓等方向，帮助你快速找到匹配岗位。",
    stats: [
      { label: "热门标签", value: "10+" },
      { label: "活跃岗位", value: "1k+" },
      { label: "岗位趋势", value: "持续增长" },
    ],
    tags: [
      "数据分析",
      "算法工程师",
      "BI",
      "Python",
      "SQL",
      "数据仓库",
      "机器学习",
      "深度学习",
      "推荐算法",
      "NLP",
    ],
  },
  {
    name: "硬件",
    subtitle: "嵌入式 / 测试 / 电子",
    icon: Lightning,
    kicker: "硬件研发",
    title: "面向研发与制造的硬核岗位",
    description: "覆盖嵌入式、硬件测试、电子工程等岗位，适合工程型人才。",
    stats: [
      { label: "热门标签", value: "7+" },
      { label: "活跃岗位", value: "600+" },
      { label: "行业方向", value: "制造 / IoT" },
    ],
    tags: [
      "嵌入式",
      "硬件工程师",
      "单片机",
      "测试工程师",
      "PCB",
      "IoT",
      "C语言",
      "ARM",
      "FPGA",
      "电路设计",
    ],
  },
  {
    name: "职能",
    subtitle: "人事 / 行政 / 财务 / 法务",
    icon: SetUp,
    kicker: "职能岗位",
    title: "稳定、专业、成长路径清晰",
    description: "适合希望在职能体系内发展的候选人，覆盖常见的后勤与支持岗位。",
    stats: [
      { label: "热门标签", value: "10+" },
      { label: "活跃岗位", value: "1k+" },
      { label: "入门门槛", value: "更友好" },
    ],
    tags: [
      "人力资源",
      "行政",
      "财务",
      "法务",
      "采购",
      "秘书",
      "招聘",
      "薪酬绩效",
      "供应链",
      "风控",
    ],
  },
];

const bannerSlides = [
  {
    title: "专注高匹配职位推荐",
    kicker: "今日推荐",
    description: "基于简历与职位语义匹配，优先展示更贴合你的岗位。",
    cover:
      "https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1600&q=80",
  },
  {
    title: "技术岗位热招中",
    kicker: "技术专区",
    description: "Java、前端、测试、算法等方向同步更新，直接进入职位页检索。",
    cover:
      "https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1600&q=80",
  },
  {
    title: "成长型企业与优质岗位",
    kicker: "企业精选",
    description: "从初创到成熟企业，筛选你更关心的行业、城市和薪资范围。",
    cover:
      "https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1600&q=80",
  },
];

const activeCategory = computed(
  () => bannerCategories[activeCategoryIndex.value] || bannerCategories[0],
);
const searchSuggestions = bannerCategories.flatMap((category) => category.tags);

const keyword = ref("");
const querySearchAsync = (queryString, cb) => {
  const normalized = (queryString || "").trim().toLowerCase();
  const results = searchSuggestions
    .filter((item, index, self) => self.indexOf(item) === index)
    .filter((item) => !normalized || item.toLowerCase().includes(normalized))
    .map((item) => ({ value: item }));
  cb(results.slice(0, 8));
};

const queryAndJump = () => {
  console.log(keyword.value);
  router.push(`/search?keyword=${keyword.value}`);
};

const handleCategoryClick = (category) => {
  if (!category || !category.tags || !category.tags.length) {
    router.push("/job");
    return;
  }
  router.push({
    path: "/job",
    query: {
      keyword: category.tags[0],
      category: category.name,
    },
  });
};

const handleTagClick = (tag) => {
  router.push({
    path: "/job",
    query: {
      keyword: tag,
    },
  });
};

const goToJobDetail = (jobId) => {
  router.push({ name: "JobDetail", params: { id: jobId } });
};

const goToCompanyDetail = (companyId) => {
  router.push({ name: "CompanyDetail", params: { id: companyId } });
};

const handleMouseEnter = (item) => {
  activeCategoryIndex.value = item;
};

const handleMouseLeave = () => {
  return;
};

const fetchHomeJobs = async () => {
  try {
    const isLoggedIn = userStore.isAuthenticated || userStore.isloginned;
    if (isLoggedIn) {
      const res = await getRecommendedJobPositionList(homeJobPageSize);
      jobList.value = res?.data?.data || [];
      return;
    }

    const res = await getJobPositionList({
      pageNum: 1,
      pageSize: homeJobPageSize,
    });
    jobList.value = res?.data?.data?.list || [];
  } catch (error) {
    console.error("获取首页职位失败:", error);
    jobList.value = [];
  }
};

const fetchHomeCompanies = async () => {
  try {
    const res = await getCompanyList({
      pageNum: 1,
      pageSize: homeCompanyPageSize,
    });
    companyList.value = res?.data?.data?.list || [];
  } catch (error) {
    console.error("获取首页公司失败:", error);
    companyList.value = [];
  }
};

onMounted(() => {
  fetchHomeJobs();
  fetchHomeCompanies();
});
</script>

<style lang="scss" scoped>
.home-page-container {
  height: 100%;
  width: 100%;
  .search-box {
    width: 80%;
    height: 5em;
    display: flex;
    justify-content: center;
    align-items: center;
    margin: 0 auto;
    .el-autocomplete {
      border-radius: 5px;
      border: 1px solid #dcdfe6;
      padding: 0 1em;
      font-size: 1em;
      &:focus {
        border-color: #409eff;
        outline: 0;
      }
    }
  }
  .banner-container {
    height: 25em;
    width: 100%;
    background: linear-gradient(135deg, #f5f7ff 0%, #eef5ff 45%, #fff7ef 100%);
    display: flex;
    .banner-menu {
      height: 100%;
      width: 30%;
      padding: 14px 10px 14px 14px;
      box-sizing: border-box;
      .el-menu-item {
        height: 4.25em;
        line-height: 1.3;
        margin-bottom: 8px;
        border-radius: 14px;
        background: rgba(255, 255, 255, 0.65);
        transition: all 0.25s ease;
        &.active,
        &:hover {
          background: #fff;
          box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12);
          transform: translateX(3px);
        }
        .menu-item-content {
          display: flex;
          flex-direction: column;
          margin-left: 10px;
          .menu-title {
            font-size: 15px;
            font-weight: 600;
            color: #303133;
          }
          .menu-subtitle {
            margin-top: 2px;
            font-size: 12px;
            color: #909399;
          }
        }
        .menu-icon {
          width: 30px;
          height: 30px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          border-radius: 10px;
          background: linear-gradient(135deg, #409eff, #66b1ff);
          color: #fff;
          font-size: 13px;
          flex-shrink: 0;
        }
      }
    }
    .banner-main {
      height: 100%;
      width: 70%;
      padding: 14px 14px 14px 0;
      box-sizing: border-box;
      .banner-hero-layout {
        height: 100%;
        width: 100%;
        display: flex;
      }
      .banner-hero-copy {
        flex: 1;
        border-radius: 20px;
        background: rgba(255, 255, 255, 0.88);
        box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
        backdrop-filter: blur(6px);
      }
      .banner-hero-copy {
        padding: 26px 28px;
        display: flex;
        flex-direction: column;
        justify-content: center;
      }
      .feature-kicker {
        margin: 0 0 10px;
        color: #409eff;
        font-weight: 700;
        letter-spacing: 0.08em;
        font-size: 13px;
      }
      .feature-description {
        margin: 12px 0 0;
        color: #606266;
        line-height: 1.8;
        max-width: 42em;
      }
      .feature-stat-row {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 12px;
        margin-top: 22px;
      }
      .feature-stat {
        padding: 14px;
        border-radius: 16px;
        background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
        border: 1px solid #e4ecff;
        strong {
          display: block;
          font-size: 18px;
          color: #303133;
        }
        span {
          display: block;
          margin-top: 4px;
          color: #909399;
          font-size: 12px;
        }
      }
      .feature-tag-list {
        margin-top: 18px;
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
      }
      .feature-tag {
        cursor: pointer;
        transition: all 0.2s ease;
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 6px 14px rgba(64, 158, 255, 0.18);
        }
      }
    }
  }
  .popular-job-container {
    margin-top: 30px;
    height: 480px;
    width: 100%;
    background-color: #f5f5f5;
    // align-items: center;
    padding: 10px;
    display: flex;
    flex-direction: column;
    .job-card-box {
      display: flex;
      justify-content: center;
      align-items: center;
      flex-wrap: wrap;
      flex: auto;
      width: 100%;
      padding: 10px;
      box-sizing: border-box;
      .job-card {
        display: inline-flex;
        flex-direction: column;
        justify-content: space-between;
        width: 210px;
        height: 190px;
        background-color: white;
        margin: 10px;
        padding: 14px;
        border-radius: 10px;
        border: 1px solid #ebeef5;
        box-sizing: border-box;
        cursor: pointer;
        transition: all 0.2s ease;
        .job-card-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          gap: 8px;
          h3 {
            margin: 0;
            color: #303133;
            font-size: 16px;
            font-weight: 600;
            line-height: 1.35;
          }
          .salary {
            margin: 0;
            color: #f56c6c;
            font-size: 13px;
            white-space: nowrap;
          }
        }
        .job-company {
          margin: 8px 0 4px;
          color: #606266;
          font-size: 13px;
        }
        .job-meta {
          display: flex;
          gap: 8px;
          color: #909399;
          font-size: 12px;
        }
        .job-manager {
          margin-top: 10px;
          display: flex;
          align-items: center;
          gap: 8px;
          color: #606266;
          font-size: 12px;
        }
        &:hover {
          border-color: #b3d8ff;
          box-shadow: 0 8px 20px rgba(64, 158, 255, 0.15);
          transform: translateY(-2px);
        }
      }
    }
  }
  .popular-company-container {
    margin-top: 40px;
    height: 560px;
    width: 100%;
    background-color: #f5f5f5;
    // align-items: center;
    padding: 10px;
    display: flex;
    flex-direction: column;
    .company-card-box {
      display: flex;
      justify-content: center;
      align-items: center;
      flex-wrap: wrap;
      flex: auto;
      width: 100%;
      padding: 10px;
      box-sizing: border-box;
      .company-card {
        display: inline-flex;
        flex-direction: column;
        width: 260px;
        height: 230px;
        background-color: white;
        margin: 10px;
        padding: 16px;
        border-radius: 12px;
        border: 1px solid #ebeef5;
        box-sizing: border-box;
        cursor: pointer;
        transition: all 0.2s ease;
        .company-head {
          display: flex;
          align-items: center;
          gap: 12px;
          .company-title {
            min-width: 0;
            h3 {
              margin: 0;
              font-size: 16px;
              color: #303133;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
            p {
              margin: 6px 0 0;
              color: #909399;
              font-size: 13px;
            }
          }
        }
        .company-tags {
          margin-top: 12px;
          display: flex;
          gap: 8px;
          flex-wrap: wrap;
        }
        .company-intro {
          margin-top: 12px;
          color: #606266;
          font-size: 13px;
          line-height: 1.5;
          overflow: hidden;
          display: -webkit-box;
          line-clamp: 3;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
        }
        &:hover {
          border-color: #c6e2ff;
          box-shadow: 0 10px 24px rgba(64, 158, 255, 0.14);
          transform: translateY(-2px);
        }
      }
    }
  }
  .more-info-btn {
    width: 240px;
    height: 3em;
    background-color: #fff;
    color: #409eff;
  }
  .more-info-btn:hover {
    background-color: #409eff;
    color: #fff;
  }
}
</style>
