import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { optimizeResumeModule } from "@/api/ai";

const createAiState = () => ({
  loading: false,
  text: "",
});

export function useResumeAiOptimization(resumeForm) {
  const summaryAiState = reactive(createAiState());
  const educationAiStates = ref([]);
  const experienceAiStates = ref([]);
  const projectAiStates = ref([]);

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

  const addModuleState = (moduleType) => {
    const state = createAiState();
    if (moduleType === "EDUCATION") {
      educationAiStates.value.push(state);
      return;
    }
    if (moduleType === "EXPERIENCE") {
      experienceAiStates.value.push(state);
      return;
    }
    if (moduleType === "PROJECT") {
      projectAiStates.value.push(state);
    }
  };

  const removeModuleState = (moduleType, moduleIndex) => {
    if (moduleType === "EDUCATION") {
      educationAiStates.value.splice(moduleIndex, 1);
      return;
    }
    if (moduleType === "EXPERIENCE") {
      experienceAiStates.value.splice(moduleIndex, 1);
      return;
    }
    if (moduleType === "PROJECT") {
      projectAiStates.value.splice(moduleIndex, 1);
    }
  };

  const getAiState = (moduleType, moduleIndex = null) => {
    if (moduleType === "SUMMARY") {
      return summaryAiState;
    }
    if (moduleType === "EDUCATION") {
      return educationAiStates.value[moduleIndex];
    }
    if (moduleType === "EXPERIENCE") {
      return experienceAiStates.value[moduleIndex];
    }
    if (moduleType === "PROJECT") {
      return projectAiStates.value[moduleIndex];
    }
    return null;
  };

  const getModuleOriginalContent = (moduleType, moduleIndex = null) => {
    if (moduleType === "SUMMARY") {
      return resumeForm.summary || "";
    }
    if (moduleType === "EDUCATION") {
      const item = resumeForm.educations[moduleIndex];
      if (!item) return "";
      return [
        item.school ? `学校：${item.school}` : "",
        item.major ? `专业：${item.major}` : "",
        item.degree ? `学历：${item.degree}` : "",
        item.description ? `描述：${item.description}` : "",
      ]
        .filter(Boolean)
        .join("\n");
    }
    if (moduleType === "EXPERIENCE") {
      const item = resumeForm.experiences[moduleIndex];
      if (!item) return "";
      return [
        item.company ? `公司：${item.company}` : "",
        item.position ? `职位：${item.position}` : "",
        item.description ? `描述：${item.description}` : "",
      ]
        .filter(Boolean)
        .join("\n");
    }
    if (moduleType === "PROJECT") {
      const item = resumeForm.projects[moduleIndex];
      if (!item) return "";
      return [
        item.name ? `项目名称：${item.name}` : "",
        item.description ? `项目描述：${item.description}` : "",
      ]
        .filter(Boolean)
        .join("\n");
    }
    return "";
  };

  const optimizeResumeSection = async (moduleType, moduleIndex = null) => {
    const originalContent = getModuleOriginalContent(moduleType, moduleIndex);
    if (!originalContent) {
      ElMessage.warning("请先填写需要优化的内容");
      return;
    }

    const aiState = getAiState(moduleType, moduleIndex);
    if (!aiState) {
      ElMessage.warning("未找到对应的优化区域");
      return;
    }

    try {
      aiState.loading = true;
      aiState.text = "";
      ElMessage.info("AI 生成中，请稍候");
      const payload = {
        moduleType,
        resumeDraft: buildResumePayload(),
      };

      if (moduleIndex !== null) {
        payload.moduleIndex = moduleIndex;
      }

      const res = await optimizeResumeModule(payload);
      const data = res?.data?.data;
      if (res?.data?.code === 200 && data?.optimizedContent) {
        aiState.text = data.optimizedContent;
      } else {
        ElMessage.error(res?.data?.message || "AI优化失败");
      }
    } catch (error) {
      ElMessage.error(error?.message || "AI优化失败，请稍后重试");
    } finally {
      aiState.loading = false;
    }
  };

  return {
    summaryAiState,
    educationAiStates,
    experienceAiStates,
    projectAiStates,
    addModuleState,
    removeModuleState,
    optimizeResumeSection,
    getModuleOriginalContent,
  };
}
