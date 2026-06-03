<template>
  <div class="resume-create-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h2>创建简历</h2>
          <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
        </div>
      </el-header>
      <el-main>
        <el-scrollbar>
          <el-form
            ref="resumeFormRef"
            :model="resumeForm"
            :rules="rules"
            label-width="120px"
            class="resume-form"
          >
            <!-- 基本信息 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>基本信息</span>
                  <el-button
                    type="primary"
                    plain
                    size="small"
                    :loading="summaryAiState.loading"
                    @click="optimizeResumeSection('SUMMARY')"
                  >
                    AI 优化简介
                  </el-button>
                </div>
              </template>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="姓名" prop="name">
                    <el-input
                      v-model="resumeForm.name"
                      placeholder="请输入姓名"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="性别" prop="gender">
                    <el-radio-group v-model="resumeForm.gender">
                      <el-radio :label="0">男</el-radio>
                      <el-radio :label="1">女</el-radio>
                      <el-radio :label="2">保密</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="出生日期" prop="birthday">
                    <el-date-picker
                      v-model="resumeForm.birthday"
                      type="date"
                      placeholder="选择出生日期"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="手机号码" prop="phone">
                    <el-input
                      v-model="resumeForm.phone"
                      placeholder="请输入手机号码"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="电子邮箱" prop="email">
                    <el-input
                      v-model="resumeForm.email"
                      placeholder="请输入电子邮箱"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="现居地址" prop="address">
                    <el-input
                      v-model="resumeForm.address"
                      placeholder="请输入现居地址"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="期望工作城市" prop="city">
                    <el-input
                      v-model="resumeForm.city"
                      placeholder="请输入期望工作城市"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="头像" prop="avatar">
                    <el-upload
                      class="avatar-uploader"
                      action="/api/upload"
                      :auto-upload="false"
                      :show-file-list="false"
                      :on-change="handleAvatarChange"
                      :before-upload="beforeAvatarUpload"
                    >
                      <img
                        v-if="resumeForm.avatar"
                        :src="resumeForm.avatar"
                        class="avatar"
                      />
                      <el-icon v-else class="avatar-uploader-icon"
                        ><Plus
                      /></el-icon>
                    </el-upload>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="个人简介/求职意向" prop="summary">
                <div class="summary-editor">
                  <el-input
                    v-model="resumeForm.summary"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入个人简介或求职意向"
                  />
                </div>
              </el-form-item>
              <ResumeAiSuggestionBox
                :loading="summaryAiState.loading"
                :text="summaryAiState.text"
              />
            </el-card>

            <!-- 教育经历 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>教育经历</span>
                  <el-button type="primary" size="small" @click="addEducation">
                    <el-icon><Plus /></el-icon> 添加教育经历
                  </el-button>
                </div>
              </template>
              <div
                v-for="(edu, index) in resumeForm.educations"
                :key="index"
                class="education-item"
              >
                <el-divider content-position="left"
                  >教育经历 {{ index + 1 }}</el-divider
                >
                <div class="module-actions">
                  <el-button
                    type="primary"
                    plain
                    size="small"
                    :loading="educationAiStates[index]?.loading"
                    @click="optimizeResumeSection('EDUCATION', index)"
                  >
                    AI 优化该经历
                  </el-button>
                </div>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item
                      :label="`学校${index + 1}`"
                      :prop="`educations.${index}.school`"
                    >
                      <el-input
                        v-model="edu.school"
                        placeholder="请输入学校名称"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item
                      :label="`专业${index + 1}`"
                      :prop="`educations.${index}.major`"
                    >
                      <el-input v-model="edu.major" placeholder="请输入专业" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item
                      :label="`学历${index + 1}`"
                      :prop="`educations.${index}.degree`"
                    >
                      <el-select v-model="edu.degree" placeholder="请选择学历">
                        <el-option label="本科" value="本科" />
                        <el-option label="硕士" value="硕士" />
                        <el-option label="博士" value="博士" />
                        <el-option label="大专" value="大专" />
                        <el-option label="高中" value="高中" />
                        <el-option label="其他" value="其他" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item
                      :label="`起止时间${index + 1}`"
                      :prop="`educations.${index}.timeRange`"
                    >
                      <el-date-picker
                        v-model="edu.timeRange"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item
                  :label="`描述${index + 1}`"
                  :prop="`educations.${index}.description`"
                >
                  <el-input
                    v-model="edu.description"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入教育经历描述"
                  />
                </el-form-item>
                <ResumeAiSuggestionBox
                  :loading="educationAiStates[index]?.loading"
                  :text="educationAiStates[index]?.text"
                />
                <el-button
                  type="danger"
                  size="small"
                  @click="removeEducation(index)"
                >
                  <el-icon><Delete /></el-icon> 删除此教育经历
                </el-button>
              </div>
            </el-card>

            <!-- 工作经历 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>工作经历</span>
                  <el-button type="primary" size="small" @click="addExperience">
                    <el-icon><Plus /></el-icon> 添加工作经历
                  </el-button>
                </div>
              </template>
              <div
                v-for="(exp, index) in resumeForm.experiences"
                :key="index"
                class="experience-item"
              >
                <el-divider content-position="left"
                  >工作经历 {{ index + 1 }}</el-divider
                >
                <div class="module-actions">
                  <el-button
                    type="primary"
                    plain
                    size="small"
                    :loading="experienceAiStates[index]?.loading"
                    @click="optimizeResumeSection('EXPERIENCE', index)"
                  >
                    AI 优化该经历
                  </el-button>
                </div>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item
                      :label="`公司${index + 1}`"
                      :prop="`experiences.${index}.company`"
                    >
                      <el-input
                        v-model="exp.company"
                        placeholder="请输入公司名称"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item
                      :label="`职位${index + 1}`"
                      :prop="`experiences.${index}.position`"
                    >
                      <el-input
                        v-model="exp.position"
                        placeholder="请输入职位"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="20">
                  <el-col :span="24">
                    <el-form-item
                      :label="`起止时间${index + 1}`"
                      :prop="`experiences.${index}.timeRange`"
                    >
                      <el-date-picker
                        v-model="exp.timeRange"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item
                  :label="`工作描述${index + 1}`"
                  :prop="`experiences.${index}.description`"
                >
                  <el-input
                    v-model="exp.description"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入工作描述"
                  />
                </el-form-item>
                <ResumeAiSuggestionBox
                  :loading="experienceAiStates[index]?.loading"
                  :text="experienceAiStates[index]?.text"
                />
                <el-button
                  type="danger"
                  size="small"
                  @click="removeExperience(index)"
                >
                  <el-icon><Delete /></el-icon> 删除此工作经历
                </el-button>
              </div>
            </el-card>

            <!-- 项目经历 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>项目经历</span>
                  <el-button type="primary" size="small" @click="addProject">
                    <el-icon><Plus /></el-icon> 添加项目经历
                  </el-button>
                </div>
              </template>
              <div
                v-for="(proj, index) in resumeForm.projects"
                :key="index"
                class="project-item"
              >
                <el-divider content-position="left"
                  >项目经历 {{ index + 1 }}</el-divider
                >
                <div class="module-actions">
                  <el-button
                    type="primary"
                    plain
                    size="small"
                    :loading="projectAiStates[index]?.loading"
                    @click="optimizeResumeSection('PROJECT', index)"
                  >
                    AI 优化该项目
                  </el-button>
                </div>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item
                      :label="`项目名称${index + 1}`"
                      :prop="`projects.${index}.name`"
                    >
                      <el-input
                        v-model="proj.name"
                        placeholder="请输入项目名称"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="20">
                  <el-col :span="24">
                    <el-form-item
                      :label="`起止时间${index + 1}`"
                      :prop="`projects.${index}.timeRange`"
                    >
                      <el-date-picker
                        v-model="proj.timeRange"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item
                  :label="`项目描述${index + 1}`"
                  :prop="`projects.${index}.description`"
                >
                  <el-input
                    v-model="proj.description"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入项目描述"
                  />
                </el-form-item>
                <ResumeAiSuggestionBox
                  :loading="projectAiStates[index]?.loading"
                  :text="projectAiStates[index]?.text"
                />
                <el-button
                  type="danger"
                  size="small"
                  @click="removeProject(index)"
                >
                  <el-icon><Delete /></el-icon> 删除此项目经历
                </el-button>
              </div>
            </el-card>

            <!-- 简历模板选择 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>选择简历模板</span>
                </div>
              </template>
              <el-form-item label="简历模板" prop="templateId">
                <el-radio-group v-model="resumeForm.templateId">
                  <el-radio
                    v-for="template in templates"
                    :key="template.id"
                    :label="template.id"
                    border
                  >
                    {{ template.name }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </el-card>

            <!-- 操作按钮 -->
            <div class="action-buttons">
              <el-button
                type="primary"
                @click="previewResume"
                :loading="previewLoading"
              >
                <el-icon><View /></el-icon> 预览简历
              </el-button>
              <el-button
                type="success"
                @click="saveResume"
                :loading="saveLoading"
              >
                <el-icon><DocumentAdd /></el-icon> 保存简历
              </el-button>
              <el-button
                type="warning"
                @click="saveAndExportResume"
                :loading="exportLoading"
              >
                <el-icon><Download /></el-icon> 保存并导出
              </el-button>
            </div>
          </el-form>
        </el-scrollbar>
      </el-main>
    </el-container>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="简历预览"
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-html="previewHtml" class="resume-preview"></div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="previewVisible = false">关闭</el-button>
          <el-button type="primary" @click="previewVisible = false"
            >确认</el-button
          >
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import {
  Plus,
  Delete,
  ArrowLeft,
  View,
  DocumentAdd,
  Download,
} from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import ResumeAiSuggestionBox from "./resume/ResumeAiSuggestionBox.vue";
import { useResumeAiOptimization } from "./resume/useResumeAiOptimization";
import {
  getResumeTemplates,
  previewUnsavedResume,
  addResume,
  getResumeDownload,
  getResumeById,
  updateResume,
} from "@/api/resume";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const resumeFormRef = ref(null);
const templates = ref([]);
const previewVisible = ref(false);
const previewHtml = ref("");
const previewLoading = ref(false);
const saveLoading = ref(false);
const exportLoading = ref(false);

// 上传图片的URL
const uploadUrl = import.meta.env.VITE_APP_BASE_API + "/file/upload";

// 简历表单数据
const resumeForm = reactive({
  userId: userStore.userId,
  templateId: 1,
  name: "",
  gender: 2,
  birthday: "",
  phone: "",
  email: "",
  address: "",
  avatar: null,
  avatarUrl: "",
  city: "",
  summary: "",
  isDefault: 0,
  educations: [],
  experiences: [],
  projects: [],
});

const {
  summaryAiState,
  educationAiStates,
  experienceAiStates,
  projectAiStates,
  addModuleState,
  removeModuleState,
  optimizeResumeSection,
} = useResumeAiOptimization(resumeForm);

// 表单验证规则
const rules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  gender: [{ required: true, message: "请选择性别", trigger: "change" }],
  phone: [
    { required: true, message: "请输入手机号码", trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入有效的手机号码",
      trigger: "blur",
    },
  ],
  email: [
    { required: true, message: "请输入电子邮箱", trigger: "blur" },
    { type: "email", message: "请输入有效的电子邮箱", trigger: "blur" },
  ],
  templateId: [
    { required: true, message: "请选择简历模板", trigger: "change" },
  ],
};

// 添加教育经历
const addEducation = () => {
  resumeForm.educations.push({
    school: "",
    major: "",
    degree: "",
    timeRange: [],
    description: "",
  });
  addModuleState("EDUCATION");
};

// 删除教育经历
const removeEducation = (index) => {
  resumeForm.educations.splice(index, 1);
  removeModuleState("EDUCATION", index);
};

// 添加工作经历
const addExperience = () => {
  resumeForm.experiences.push({
    company: "",
    position: "",
    timeRange: [],
    description: "",
  });
  addModuleState("EXPERIENCE");
};

// 删除工作经历
const removeExperience = (index) => {
  resumeForm.experiences.splice(index, 1);
  removeModuleState("EXPERIENCE", index);
};

// 添加项目经历
const addProject = () => {
  resumeForm.projects.push({
    name: "",
    timeRange: [],
    description: "",
  });
  addModuleState("PROJECT");
};

// 删除项目经历
const removeProject = (index) => {
  resumeForm.projects.splice(index, 1);
  removeModuleState("PROJECT", index);
};

// 处理头像文件选择
const handleAvatarChange = (file) => {
  const isJPG = file.raw.type === "image/jpeg" || file.raw.type === "image/png";
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG) {
    ElMessage.error("上传头像只能是JPG/PNG格式!");
    return false;
  }
  if (!isLt2M) {
    ElMessage.error("上传头像大小不能超过2MB!");
    return false;
  }

  // 预览图片
  resumeForm.avatar = URL.createObjectURL(file.raw);
  return true;
};

// 头像上传前校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === "image/jpeg" || file.type === "image/png";
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG) {
    ElMessage.error("上传头像图片只能是 JPG/PNG 格式!");
  }
  if (!isLt2M) {
    ElMessage.error("上传头像图片大小不能超过 2MB!");
  }
  return isJPG && isLt2M;
};

const buildResumePayload = () => ({
  ...resumeForm,
  educations: resumeForm.educations.map((edu) => ({
    ...edu,
    startDate: edu.timeRange?.[0],
    endDate: edu.timeRange?.[1],
  })),
  experiences: resumeForm.experiences.map((exp) => ({
    ...exp,
    startDate: exp.timeRange?.[0],
    endDate: exp.timeRange?.[1],
  })),
  projects: resumeForm.projects.map((proj) => ({
    ...proj,
    startDate: proj.timeRange?.[0],
    endDate: proj.timeRange?.[1],
  })),
});

// 预览简历
const previewResume = async () => {
  try {
    await resumeFormRef.value.validate();
    previewLoading.value = true;

    // 准备请求数据
    const requestData = {
      ...resumeForm,
      educations: resumeForm.educations.map((edu) => ({
        ...edu,
        startDate: edu.timeRange[0],
        endDate: edu.timeRange[1],
      })),
      experiences: resumeForm.experiences.map((exp) => ({
        ...exp,
        startDate: exp.timeRange[0],
        endDate: exp.timeRange[1],
      })),
      projects: resumeForm.projects.map((proj) => ({
        ...proj,
        startDate: proj.timeRange[0],
        endDate: proj.timeRange[1],
      })),
    };

    const res = await previewUnsavedResume(requestData, resumeForm.templateId);
    if (res.data.code === 200 && res.data.data) {
      previewHtml.value = res.data.data;
      previewVisible.value = true;
    } else {
      ElMessage.error(res.msg || "预览简历失败");
    }
  } catch (error) {
    if (error !== false) {
      ElMessage.error("预览简历失败，请检查表单信息");
    }
  } finally {
    previewLoading.value = false;
  }
};

// 保存简历
const saveResume = async () => {
  try {
    await resumeFormRef.value.validate();
    saveLoading.value = true;
    const avatarInput = document.querySelector(
      '.avatar-uploader input[type="file"]',
    );
    const avatarFile =
      avatarInput && avatarInput.files[0] ? avatarInput.files[0] : null;

    const requestData = buildResumePayload();

    if (route.query.resumeId) {
      // 编辑模式
      const id = route.query.resumeId;
      const res = await updateResume(id, requestData, avatarFile);
      if (res?.data?.code === 200) {
        ElMessage.success("简历更新成功");
        router.push("/resume");
      } else {
        ElMessage.error(res?.data?.message || "更新简历失败");
      }
    } else {
      // 新增模式
      const res = await addResume(requestData, avatarFile);
      if (res?.data?.code === 200) {
        ElMessage.success("简历保存成功");
        router.push("/resume");
      } else {
        ElMessage.error(res?.data?.message || "保存简历失败");
      }
    }
  } catch (error) {
    if (error !== false) {
      ElMessage.error("保存简历失败，请检查表单信息");
    }
  } finally {
    saveLoading.value = false;
  }
};

const fillFormFromVo = (vo) => {
  if (!vo) return;
  resumeForm.userId = vo.userId || resumeForm.userId;
  resumeForm.templateId = vo.templateId || resumeForm.templateId;
  resumeForm.name = vo.name || "";
  resumeForm.gender = vo.gender ?? resumeForm.gender;
  resumeForm.birthday = vo.birthday ? String(vo.birthday).slice(0, 10) : "";
  resumeForm.phone = vo.phone || "";
  resumeForm.email = vo.email || "";
  resumeForm.address = vo.address || "";
  resumeForm.avatar = vo.avatar || null;
  resumeForm.city = vo.city || "";
  resumeForm.summary = vo.summary || "";
  resumeForm.isDefault = vo.isDefault ?? 0;

  // clear arrays then fill
  resumeForm.educations = [];
  resumeForm.experiences = [];
  resumeForm.projects = [];

  if (Array.isArray(vo.educations)) {
    vo.educations.forEach((e) => {
      resumeForm.educations.push({
        school: e.school || "",
        major: e.major || "",
        degree: e.degree || "",
        timeRange: [
          e.startDate ? String(e.startDate).slice(0, 10) : "",
          e.endDate ? String(e.endDate).slice(0, 10) : "",
        ],
        description: e.description || "",
      });
      addModuleState("EDUCATION");
    });
  }

  if (Array.isArray(vo.experiences)) {
    vo.experiences.forEach((ex) => {
      resumeForm.experiences.push({
        company: ex.company || "",
        position: ex.position || "",
        timeRange: [
          ex.startDate ? String(ex.startDate).slice(0, 10) : "",
          ex.endDate ? String(ex.endDate).slice(0, 10) : "",
        ],
        description: ex.description || "",
      });
      addModuleState("EXPERIENCE");
    });
  }

  if (Array.isArray(vo.projects)) {
    vo.projects.forEach((p) => {
      resumeForm.projects.push({
        name: p.name || "",
        timeRange: [
          p.startDate ? String(p.startDate).slice(0, 10) : "",
          p.endDate ? String(p.endDate).slice(0, 10) : "",
        ],
        description: p.description || "",
      });
      addModuleState("PROJECT");
    });
  }
};

// 保存并导出简历
const saveAndExportResume = async () => {
  try {
    await resumeFormRef.value.validate();
    exportLoading.value = true;

    const avatarInput = document.querySelector(
      '.avatar-uploader input[type="file"]',
    );
    const avatarFile =
      avatarInput && avatarInput.files[0] ? avatarInput.files[0] : null;

    // 先保存简历
    const saveRes = await addResume(buildResumePayload(), avatarFile);
    if (saveRes?.data?.code === 200) {
      const resumeId = saveRes?.data?.data;

      if (!resumeId) {
        ElMessage.warning("保存成功，但接口未返回简历ID，无法继续导出");
        return;
      }

      // 导出简历（与 ApiTest 一致的 PDF 检查和下载逻辑）
      const exportRes = await getResumeDownload(
        resumeId,
        resumeForm.templateId,
      );
      const contentType = exportRes?.headers?.["content-type"] || "";
      if (!contentType.includes("application/pdf")) {
        const errorText = await exportRes?.data?.text?.();
        ElMessage.error(errorText || "导出失败，返回的不是PDF文件");
        return;
      }

      if (exportRes?.data) {
        const blob = exportRes.data;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute(
          "download",
          `resume_${resumeId}_tpl_${resumeForm.templateId}.pdf`,
        );
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        ElMessage.success("简历保存并导出成功");
        router.push("/resume");
      } else {
        ElMessage.error("导出简历失败");
      }
    } else {
      ElMessage.error(saveRes?.data?.message || "保存简历失败");
    }
  } catch (error) {
    console.error("API Error:", error);
    if (error !== false) {
      ElMessage.error("保存并导出简历失败，请检查表单信息");
    }
  } finally {
    exportLoading.value = false;
  }
};

// 返回上一页
const goBack = () => {
  router.push("/resume");
};

// 获取简历模板列表
const fetchTemplates = async () => {
  try {
    const res = await getResumeTemplates();
    if (res.data.code === 200 && res.data.data) {
      templates.value = res.data.data;
      if (templates.value.length > 0) {
        resumeForm.templateId = templates.value[0].id;
      }
    }
  } catch (error) {
    ElMessage.error("获取简历模板失败");
  }
};

// 组件挂载时获取模板列表
onMounted(() => {
  fetchTemplates();
  // 如果是编辑模式，获取已保存的简历并填充表单
  const resumeId = route.query.resumeId;
  if (resumeId) {
    (async () => {
      try {
        const res = await getResumeById(resumeId);
        if (res?.data?.code === 200 && res?.data?.data) {
          fillFormFromVo(res.data.data);
        } else {
          ElMessage.error("获取简历数据失败");
        }
      } catch (err) {
        ElMessage.error("获取简历数据失败");
      }
    })();
  }
  document.body.style.overflow = "hidden";
});

onBeforeUnmount(() => {
  document.body.style.overflow = "";
});
</script>

<style lang="scss" scoped>
.resume-create-container {
  width: 100%;
  height: 100vh;
  display: flex;
  overflow: hidden;

  .el-container {
    .el-header {
      background-color: #f5f7fa;
      display: flex;
      align-items: center;
      padding: 0 20px;
      border-bottom: 1px solid #e4e7ed;

      .header-content {
        width: 100%;
        display: flex;
        justify-content: space-between;
        align-items: center;

        h2 {
          margin: 0;
          font-size: 20px;
          color: #303133;
        }
      }
    }

    .el-main {
      background-color: #f5f7fa;
      padding: 20px;

      .resume-form {
        background-color: #fff;
        padding: 20px;
        border-radius: 4px;

        .section-card {
          margin-bottom: 20px;

          .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;

            span {
              font-size: 16px;
              font-weight: bold;
            }
          }

          .education-item,
          .experience-item,
          .project-item {
            margin-bottom: 20px;
            padding: 10px;
            background-color: #f9f9f9;
            border-radius: 4px;

            .module-actions {
              display: flex;
              justify-content: flex-end;
              margin-bottom: 12px;
            }
          }
        }

        .action-buttons {
          display: flex;
          justify-content: center;
          gap: 20px;
          margin-top: 30px;
          padding-bottom: 20px;
        }
      }
    }
  }

  .avatar-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: all 0.3s;

      &:hover {
        border-color: #409eff;
      }
    }
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 178px;
    height: 178px;
    line-height: 178px;
    text-align: center;
  }

  .avatar {
    width: 178px;
    height: 178px;
    display: block;
  }

  .resume-preview {
    width: 100%;
    min-height: 500px;
    border: 1px solid #e4e7ed;
    padding: 20px;
    background-color: #fff;
    overflow: auto;
  }

  .summary-editor {
    width: 100%;
  }

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
}
</style>
