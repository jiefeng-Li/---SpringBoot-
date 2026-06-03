<template>
  <div class="interview-notice-list">
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
        <el-table-column prop="companyName" label="公司" min-width="160" />
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
            <el-tag :type="statusTagType(row.status)">{{
              statusLabel(row.status)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openDetail(row)"
              >查看</el-button
            >
            <el-button
              v-if="canRespond(row.status)"
              type="success"
              link
              @click.stop="acceptNotice(row)"
              >同意</el-button
            >
            <el-button
              v-if="canRespond(row.status)"
              type="danger"
              link
              @click.stop="rejectNotice(row)"
              >拒绝</el-button
            >
            <el-button
              v-if="canJoin(row)"
              type="warning"
              link
              @click.stop="joinMeeting(row)"
              >加入面试</el-button
            >
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
      title="面试通知详情"
      width="860px"
      destroy-on-close
    >
      <template v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="公司">{{
            currentRow.companyName || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{
            currentRow.creatorName || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{
            currentRow.interviewTypeText || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentRow.status)">{{
              statusLabel(currentRow.status)
            }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{
            currentRow.interviewStartTime || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{
            currentRow.interviewEndTime || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="线下地址" :span="2">{{
            currentRow.interviewAddress || "-"
          }}</el-descriptions-item>
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
              >{{ currentRow.rtcJoinUrl }}</el-link
            >
            <el-link
              v-else-if="
                currentRow.interviewType === 1 &&
                (currentRow.rtcRoomId || currentRow.id)
              "
              type="primary"
              @click="joinMeeting(currentRow)"
            >
              使用系统入会页加入
            </el-link>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="面试官" :span="2">
            <el-space wrap>
              <el-tag
                v-for="item in currentRow.interviewers || []"
                :key="item.userId"
                >{{ item.username }}</el-tag
              >
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
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="canRespond(currentRow?.status)"
          type="success"
          :loading="acceptLoading"
          @click="acceptNotice(currentRow)"
          >同意面试</el-button
        >
        <el-button
          v-if="canRespond(currentRow?.status)"
          type="danger"
          plain
          :loading="rejectLoading"
          @click="rejectNotice(currentRow)"
          >拒绝面试</el-button
        >
        <el-button
          v-if="canJoin(currentRow)"
          type="warning"
          @click="joinMeeting(currentRow)"
          >加入面试</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getOwnInterviewNoticeList,
  respondInterviewNotice,
} from "@/api/interview";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();
const router = useRouter();

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
const acceptLoading = ref(false);
const rejectLoading = ref(false);

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
    default:
      return "";
  }
};

const canRespond = (status) => status === 0;
const canJoin = (row) =>
  row?.status === 1 &&
  row?.interviewType === 1 &&
  !!(row?.rtcRoomId || row?.id);

const dialogTitle = computed(() =>
  currentRow.value
    ? `面试通知详情 - ${currentRow.value.companyName || ""}`
    : "面试通知详情",
);

const fetchList = async () => {
  if (!userStore.userId) {
    ElMessage.warning("未获取到用户信息，请重新登录后重试");
    return;
  }
  tableLoading.value = true;
  try {
    const { data } = await getOwnInterviewNoticeList({
      status: query.status,
      interviewType: query.interviewType,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    });
    const page = data?.data || data || {};
    tableData.value = page.records || page.list || [];
    total.value = page.total || 0;
  } catch (e) {
    ElMessage.error(e?.message || "获取面试通知失败");
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

const openDetail = (row) => {
  currentRow.value = row;
  detailVisible.value = true;
};

const acceptNotice = async (row) => {
  if (!row?.id) {
    ElMessage.error("面试记录ID不存在");
    return;
  }
  try {
    acceptLoading.value = true;
    await respondInterviewNotice({ id: row.id, status: 1 });
    ElMessage.success("已同意面试通知");
    detailVisible.value = false;
    fetchList();
  } catch (e) {
    ElMessage.error(e?.message || "操作失败");
  } finally {
    acceptLoading.value = false;
  }
};

const rejectNotice = async (row) => {
  if (!row?.id) {
    ElMessage.error("面试记录ID不存在");
    return;
  }
  try {
    const { value } = await ElMessageBox.prompt("请输入拒绝原因", "拒绝面试", {
      confirmButtonText: "确认拒绝",
      cancelButtonText: "返回",
      inputPlaceholder: "例如：时间不合适",
      inputValidator: (text) =>
        (text && text.trim().length > 0) || "请填写拒绝原因",
    });
    rejectLoading.value = true;
    await respondInterviewNotice({
      id: row.id,
      status: 2,
      rejectReason: value.trim(),
    });
    ElMessage.success("已拒绝面试通知");
    detailVisible.value = false;
    fetchList();
  } catch (e) {
    if (e !== "cancel") {
      ElMessage.error(e?.message || "拒绝失败");
    }
  } finally {
    rejectLoading.value = false;
  }
};

const joinMeeting = (row) => {
  if (!row?.id) {
    ElMessage.warning("当前面试记录不存在");
    return;
  }
  const noticeId = String(row.id);
  console.log("加入面试记录：", row);
  router.push("/meeting/trtc?noticeId=" + noticeId);
};

onMounted(() => {
  fetchList();
});
</script>

<style lang="scss" scoped>
.interview-notice-list {
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
}
</style>
