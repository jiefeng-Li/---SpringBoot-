<template>
  <div class="interview-records">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部状态"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="类型">
          <el-select
            v-model="query.interviewType"
            placeholder="全部类型"
            clearable
            style="width: 180px"
          >
            <el-option label="线上面试" :value="1" />
            <el-option label="线下面试" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
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
        <el-table-column prop="creatorName" label="创建人" min-width="120" />
        <el-table-column prop="interviewTypeText" label="类型" width="110" />
        <el-table-column
          prop="interviewStartTime"
          label="开始时间"
          min-width="180"
        />
        <el-table-column
          prop="interviewEndTime"
          label="结束时间"
          min-width="180"
        />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openDetail(row)"
              >查看</el-button
            >
            <el-button
              v-if="canEdit(row.status)"
              type="warning"
              link
              @click.stop="openEdit(row)"
            >
              编辑并重发
            </el-button>
            <el-button
              v-if="canCancel(row.status)"
              type="danger"
              link
              @click.stop="handleCancel(row)"
            >
              取消
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
      :title="dialogTitle"
      width="920px"
      destroy-on-close
    >
      <template v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="求职者">{{
            currentRow.jobSeekerName || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{
            currentRow.creatorName || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{
            currentRow.interviewTypeText || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentRow.status)">
              {{ statusLabel(currentRow.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{
            currentRow.interviewStartTime || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{
            currentRow.interviewEndTime || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="线下地址" :span="2">
            {{ currentRow.interviewAddress || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="会议平台">{{
            currentRow.rtcPlatformText || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="房间号">{{
            currentRow.rtcRoomId || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="会议名称">{{
            currentRow.rtcRoomName || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="加入链接" :span="2">
            <el-link
              v-if="currentRow.rtcJoinUrl"
              :href="currentRow.rtcJoinUrl"
              target="_blank"
            >
              {{ currentRow.rtcJoinUrl }}
            </el-link>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="面试官" :span="2">
            <el-space wrap>
              <el-tag
                v-for="item in currentRow.interviewers || []"
                :key="item.userId"
              >
                {{ item.username }}
              </el-tag>
            </el-space>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{
            currentRow.comment || "-"
          }}</el-descriptions-item>
          <el-descriptions-item
            v-if="currentRow.candidateReplyReason"
            label="拒绝原因"
            :span="2"
          >
            {{ currentRow.candidateReplyReason }}
          </el-descriptions-item>
          <el-descriptions-item
            v-if="currentRow.cancelReason"
            label="取消原因"
            :span="2"
          >
            {{ currentRow.cancelReason }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="edit-wrap" v-if="canEdit(currentRow.status)">
          <div class="section-title">重新安排面试</div>
          <el-form :model="editForm" label-width="110px">
            <el-form-item label="面试类型" required>
              <el-radio-group v-model="editForm.interviewType">
                <el-radio :label="1">线上面试</el-radio>
                <el-radio :label="0">线下面试</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="面试时间" required>
              <el-date-picker
                v-model="editForm.interviewTimeRange"
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
                v-model="editForm.interviewerIds"
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

            <template v-if="editForm.interviewType === 1">
              <el-form-item label="会议平台" required>
                <el-select
                  v-model="editForm.rtcPlatform"
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
                <el-input :value="String(editForm.id || '')" disabled />
              </el-form-item>
              <el-form-item label="会议名称">
                <el-input
                  v-model="editForm.rtcRoomName"
                  placeholder="例如：前端岗位一面"
                  maxlength="128"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="加入链接">
                <el-input
                  v-model="editForm.rtcJoinUrl"
                  placeholder="可选，留空将使用系统内嵌入会页"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="入会口令">
                <el-input
                  v-model="editForm.rtcPassword"
                  placeholder="如有口令可填写"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="面试地址" required>
                <el-input
                  v-model="editForm.interviewAddress"
                  placeholder="请输入详细面试地址"
                  maxlength="255"
                  show-word-limit
                />
              </el-form-item>
            </template>

            <el-form-item label="备注">
              <el-input
                v-model="editForm.comment"
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
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="currentRow && canEdit(currentRow.status)">
          <el-button
            type="warning"
            :loading="updateLoading"
            @click="submitUpdate"
          >
            更新并重新发起
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="cancelLoading"
            @click="handleCancel(currentRow)"
          >
            取消面试
          </el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getInterviewNoticeList,
  updateInterviewNotice,
  cancelInterviewNotice,
} from "@/api/interview";
import { getCompanyUserList } from "@/api/company";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();

const statusOptions = [
  { label: "待确认", value: 0 },
  { label: "已确认", value: 1 },
  { label: "已拒绝", value: 2 },
  { label: "已取消", value: 3 },
  { label: "已结束", value: 4 },
];

const query = reactive({
  status: undefined,
  interviewType: undefined,
  pageNum: 1,
  pageSize: 10,
});

const tableLoading = ref(false);
const tableData = ref([]);
const total = ref(0);

const detailVisible = ref(false);
const currentRow = ref(null);
const updateLoading = ref(false);
const cancelLoading = ref(false);

const interviewerLoading = ref(false);
const interviewerOptions = ref([]);

const editForm = reactive({
  id: null,
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

const statusLabel = (status) =>
  statusOptions.find((item) => item.value === status)?.label ||
  `未知(${status})`;

const statusTagType = (status) => {
  switch (status) {
    case 0:
      return "info";
    case 1:
      return "success";
    case 2:
      return "danger";
    case 3:
      return "warning";
    case 4:
      return "";
    default:
      return "info";
  }
};

const canEdit = (status) => [0, 2, 3].includes(status);
const canCancel = (status) => [0, 2].includes(status);

const dialogTitle = computed(() => {
  if (!currentRow.value) return "面试详情";
  return canEdit(currentRow.value.status) ? "面试详情与重新发起" : "面试详情";
});

const fetchInterviewers = async () => {
  if (!userStore.companyId) {
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
  if (!userStore.companyId) {
    ElMessage.warning("未获取到公司ID，请重新登录后重试");
    return;
  }
  tableLoading.value = true;
  try {
    const { data } = await getInterviewNoticeList({
      status: query.status,
      interviewType: query.interviewType,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    });
    const page = data?.data || data || {};
    tableData.value = page.records || page.list || [];
    total.value = page.total || 0;
  } catch (e) {
    ElMessage.error(e?.message || "获取面试记录失败");
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => {
  query.pageNum = 1;
  fetchList();
};

const handleReset = () => {
  query.status = undefined;
  query.interviewType = undefined;
  query.pageNum = 1;
  query.pageSize = 10;
  fetchList();
};

const handleSizeChange = () => {
  query.pageNum = 1;
  fetchList();
};

const resetEditForm = () => {
  editForm.id = null;
  editForm.interviewType = 1;
  editForm.interviewTimeRange = [];
  editForm.interviewerIds = [];
  editForm.interviewAddress = "";
  editForm.rtcPlatform = 1;
  editForm.rtcRoomId = "";
  editForm.rtcRoomName = "";
  editForm.rtcJoinUrl = "";
  editForm.rtcPassword = "";
  editForm.comment = "";
};

const populateEditForm = (row) => {
  editForm.id = row.id;
  editForm.interviewType = row.interviewType ?? 1;
  editForm.interviewTimeRange = [row.interviewStartTime, row.interviewEndTime];
  editForm.interviewerIds = (row.interviewers || []).map((item) => item.userId);
  editForm.interviewAddress = row.interviewAddress || "";
  editForm.rtcPlatform = row.rtcPlatform || 1;
  editForm.rtcRoomId = row.rtcRoomId || "";
  editForm.rtcRoomName = row.rtcRoomName || "";
  editForm.rtcJoinUrl = row.rtcJoinUrl || "";
  editForm.rtcPassword = row.rtcPassword || "";
  editForm.comment = row.comment || "";
};

const openDetail = (row) => {
  currentRow.value = row;
  if (row && canEdit(row.status)) {
    populateEditForm(row);
  } else {
    resetEditForm();
  }
  detailVisible.value = true;
};

const openEdit = (row) => {
  currentRow.value = row;
  populateEditForm(row);
  detailVisible.value = true;
};

const submitUpdate = async () => {
  if (!editForm.id) {
    ElMessage.error("面试记录ID不存在");
    return;
  }
  if (
    !Array.isArray(editForm.interviewTimeRange) ||
    editForm.interviewTimeRange.length !== 2
  ) {
    ElMessage.warning("请选择完整的面试时间范围");
    return;
  }
  if (!editForm.interviewerIds.length) {
    ElMessage.warning("请至少选择一个面试官");
    return;
  }

  if (editForm.interviewType === 1) {
  } else if (!editForm.interviewAddress.trim()) {
    ElMessage.warning("请输入面试地址");
    return;
  }

  updateLoading.value = true;
  try {
    await updateInterviewNotice({
      id: editForm.id,
      interviewType: editForm.interviewType,
      interviewStartTime: editForm.interviewTimeRange[0],
      interviewEndTime: editForm.interviewTimeRange[1],
      interviewAddress:
        editForm.interviewType === 0 ? editForm.interviewAddress.trim() : "",
      rtcPlatform: editForm.interviewType === 1 ? editForm.rtcPlatform : null,
      rtcRoomId: editForm.interviewType === 1 ? String(editForm.id) : "",
      rtcRoomName:
        editForm.interviewType === 1 ? editForm.rtcRoomName.trim() : "",
      rtcJoinUrl:
        editForm.interviewType === 1 ? editForm.rtcJoinUrl.trim() : "",
      rtcPassword:
        editForm.interviewType === 1 ? editForm.rtcPassword.trim() : "",
      comment: editForm.comment?.trim() || "",
      interviewerIds: editForm.interviewerIds,
    });
    ElMessage.success("面试信息更新成功");
    detailVisible.value = false;
    fetchList();
  } catch (e) {
    ElMessage.error(e?.message || "更新面试信息失败");
  } finally {
    updateLoading.value = false;
  }
};

const handleCancel = async (row) => {
  if (!row?.id) {
    ElMessage.error("面试记录ID不存在");
    return;
  }

  try {
    const { value } = await ElMessageBox.prompt("请输入取消原因", "取消面试", {
      confirmButtonText: "确认取消",
      cancelButtonText: "返回",
      inputPlaceholder: "例如：招聘计划调整",
      inputValidator: (text) =>
        (text && text.trim().length > 0) || "请填写取消原因",
    });
    cancelLoading.value = true;
    await cancelInterviewNotice({
      id: row.id,
      cancelReason: value.trim(),
    });
    ElMessage.success("面试已取消");
    detailVisible.value = false;
    fetchList();
  } catch (e) {
    if (e !== "cancel") {
      ElMessage.error(e?.message || "取消面试失败");
    }
  } finally {
    cancelLoading.value = false;
  }
};

onMounted(() => {
  fetchList();
  fetchInterviewers();
});
</script>

<style lang="scss" scoped>
.interview-records {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .filter-card,
  .table-card {
    .pager {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .edit-wrap {
    margin-top: 18px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;

    .section-title {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 12px;
    }
  }
}
</style>
