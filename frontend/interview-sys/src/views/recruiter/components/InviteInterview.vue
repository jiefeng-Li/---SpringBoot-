<template>
  <div class="invite-interview">
    <el-card shadow="never" class="tip-card">
      <el-alert
        title="仅展示状态为“初筛通过”的投递记录。创建成功后，后端会自动生成面试预约并将投递推进到“面试中”。"
        type="info"
        :closable="false"
      />
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        v-loading="tableLoading"
        border
        stripe
        @row-click="openDetail"
      >
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="jobSeekerName" label="求职者" min-width="120" />
        <el-table-column prop="jobTitle" label="职位" min-width="180" />
        <el-table-column prop="applyTime" label="投递时间" min-width="170" />
        <el-table-column
          prop="remarks"
          label="备注"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openDetail(row)">
              查看并邀约
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="fetchList"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="投递详情与面试邀约"
      width="860px"
      destroy-on-close
    >
      <template v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="投递ID">
            {{ currentRow.id || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="求职者">
            {{ currentRow.jobSeekerName || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="职位">
            {{ currentRow.jobTitle || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag type="success">初筛通过</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="投递时间">
            {{ currentRow.applyTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="投递备注">
            {{ currentRow.remarks || "-" }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="invite-form-wrap">
          <div class="section-title">填写面试信息</div>
          <el-form :model="inviteForm" label-width="110px">
            <el-form-item label="面试类型" required>
              <el-radio-group v-model="inviteForm.interviewType">
                <el-radio :label="1">线上面试</el-radio>
                <el-radio :label="0">线下面试</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="面试时间" required>
              <el-date-picker
                v-model="inviteForm.interviewTimeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                format="YYYY-MM-DD HH:mm"
                style="width: 420px"
              />
            </el-form-item>

            <el-form-item label="面试官" required>
              <el-select
                v-model="inviteForm.interviewerIds"
                multiple
                filterable
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择本公司面试官"
                :loading="interviewerLoading"
                style="width: 420px"
              >
                <el-option
                  v-for="item in interviewerOptions"
                  :key="item.userId"
                  :label="`${item.username}（${item.userId}）`"
                  :value="item.userId"
                />
              </el-select>
            </el-form-item>

            <template v-if="inviteForm.interviewType === 1">
              <el-form-item label="会议平台" required>
                <el-select
                  v-model="inviteForm.rtcPlatform"
                  style="width: 220px"
                  placeholder="请选择会议平台"
                >
                  <el-option label="腾讯云 TRTC" :value="1" />
                  <el-option label="ZEGO 即构" :value="2" />
                  <el-option label="Agora" :value="3" />
                  <el-option label="Jitsi" :value="4" />
                </el-select>
              </el-form-item>
              <el-form-item label="房间号">
                <el-input value="系统自动生成（使用面试记录ID）" disabled />
              </el-form-item>
              <el-form-item label="会议名称">
                <el-input
                  v-model="inviteForm.rtcRoomName"
                  placeholder="例如：前端岗位一面"
                  maxlength="128"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="加入链接">
                <el-input
                  v-model="inviteForm.rtcJoinUrl"
                  placeholder="可选，留空将使用系统内嵌入会页"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="入会口令">
                <el-input
                  v-model="inviteForm.rtcPassword"
                  placeholder="如有口令可填写"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="面试地址" required>
                <el-input
                  v-model="inviteForm.interviewAddress"
                  placeholder="请输入详细面试地址"
                  maxlength="255"
                  show-word-limit
                />
              </el-form-item>
            </template>

            <el-form-item label="备注">
              <el-input
                v-model="inviteForm.comment"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="可填写面试说明、联系人、设备要求等信息"
              />
            </el-form-item>
          </el-form>
        </div>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submitInvite"
        >
          发送面试通知
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getJobApplicationList } from "@/api/application";
import { addInterviewNotice } from "@/api/interview";
import { getCompanyUserList } from "@/api/company";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();

const query = reactive({
  companyId: userStore?.companyId || undefined,
  status: 2,
  pageNum: 1,
  pageSize: 10,
});

const tableLoading = ref(false);
const tableData = ref([]);
const total = ref(0);

const detailVisible = ref(false);
const currentRow = ref(null);
const submitLoading = ref(false);
const interviewerLoading = ref(false);
const interviewerOptions = ref([]);

const inviteForm = reactive({
  interviewType: 1,
  interviewTimeRange: [],
  interviewerIds: [],
  interviewAddress: "",
  rtcPlatform: 1,
  rtcRoomId: "",
  rtcRoomName: "",
  rtcJoinUrl: "",
  rtcPassword: "",
  comment: "",
});

const fetchInterviewers = async () => {
  if (!query.companyId) {
    return;
  }

  interviewerLoading.value = true;
  try {
    const { data } = await getCompanyUserList({
      pageNum: 1,
      pageSize: 100,
      role: "RECRUITER",
    });
    const page = data?.data || data || {};
    interviewerOptions.value = page.records || page.list || [];
  } catch (e) {
    ElMessage.error(e?.message || "获取公司面试官失败");
  } finally {
    interviewerLoading.value = false;
  }
};

const fetchList = async () => {
  if (!query.companyId) {
    ElMessage.warning("未获取到公司ID，请重新登录后重试");
    return;
  }

  tableLoading.value = true;
  try {
    const { data } = await getJobApplicationList({
      companyId: query.companyId,
      status: 2,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    });

    const page = data?.data || data || {};
    tableData.value = page.records || page.list || [];
    total.value = page.total || 0;
  } catch (e) {
    ElMessage.error(e?.message || "获取初筛通过投递记录失败");
  } finally {
    tableLoading.value = false;
  }
};

const handleSizeChange = () => {
  query.pageNum = 1;
  fetchList();
};

const resetInviteForm = () => {
  inviteForm.interviewType = 1;
  inviteForm.interviewTimeRange = [];
  inviteForm.interviewerIds = [];
  inviteForm.interviewAddress = "";
  inviteForm.rtcPlatform = 1;
  inviteForm.rtcRoomId = "";
  inviteForm.rtcRoomName = "";
  inviteForm.rtcJoinUrl = "";
  inviteForm.rtcPassword = "";
  inviteForm.comment = "";
};

const openDetail = (row) => {
  currentRow.value = row;
  resetInviteForm();
  detailVisible.value = true;
};

const submitInvite = async () => {
  if (!currentRow.value?.id) {
    ElMessage.error("投递记录ID不存在");
    return;
  }
  if (
    !Array.isArray(inviteForm.interviewTimeRange) ||
    inviteForm.interviewTimeRange.length !== 2
  ) {
    ElMessage.warning("请选择完整的面试时间范围");
    return;
  }
  if (!inviteForm.interviewerIds.length) {
    ElMessage.warning("请至少选择一个面试官");
    return;
  }

  if (inviteForm.interviewType === 1) {
    if (!inviteForm.rtcPlatform) {
      ElMessage.warning("请选择会议平台");
      return;
    }
  } else if (!inviteForm.interviewAddress.trim()) {
    ElMessage.warning("请输入面试地址");
    return;
  }

  submitLoading.value = true;
  try {
    await addInterviewNotice({
      jobApplicationId: currentRow.value.id,
      interviewType: inviteForm.interviewType,
      interviewStartTime: inviteForm.interviewTimeRange[0],
      interviewEndTime: inviteForm.interviewTimeRange[1],
      interviewAddress:
        inviteForm.interviewType === 0
          ? inviteForm.interviewAddress.trim()
          : "",
      rtcPlatform:
        inviteForm.interviewType === 1 ? inviteForm.rtcPlatform : null,
      rtcRoomId:
        inviteForm.interviewType === 1 ? inviteForm.rtcRoomId.trim() : "",
      rtcRoomName:
        inviteForm.interviewType === 1 ? inviteForm.rtcRoomName.trim() : "",
      rtcJoinUrl:
        inviteForm.interviewType === 1 ? inviteForm.rtcJoinUrl.trim() : "",
      rtcPassword:
        inviteForm.interviewType === 1 ? inviteForm.rtcPassword.trim() : "",
      comment: inviteForm.comment?.trim() || "",
      interviewerIds: inviteForm.interviewerIds,
    });

    ElMessage.success("面试通知发送成功");
    detailVisible.value = false;
    fetchList();
  } catch (e) {
    ElMessage.error(e?.message || "发送面试通知失败");
  } finally {
    submitLoading.value = false;
  }
};

onMounted(() => {
  fetchList();
  fetchInterviewers();
});
</script>

<style lang="scss" scoped>
.invite-interview {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .tip-card {
    :deep(.el-card__body) {
      padding: 12px;
    }
  }

  .table-card {
    .pager {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .invite-form-wrap {
    margin-top: 16px;

    .section-title {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 12px;
    }
  }
}
</style>
