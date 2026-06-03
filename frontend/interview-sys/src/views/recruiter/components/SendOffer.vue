<template>
  <div class="send-offer">
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <span class="filter-label">快捷筛选</span>
        <el-radio-group v-model="quickFilter" @change="handleQuickFilterChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="interviewing">仅面试中</el-radio-button>
          <el-radio-button label="offerSent">仅已发Offer</el-radio-button>
          <el-radio-button label="hired">仅已录用</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <el-card shadow="never" class="tip-card">
      <div class="tip-row">
        <el-alert
          title="展示状态为“面试中”和“已发 Offer”的投递记录。对于拒绝的 Offer 可一键清理为“已淘汰”。"
          type="info"
          :closable="false"
        />
        <el-button
          type="danger"
          plain
          :loading="cleanLoading"
          @click="cleanRejectedOffers"
        >
          一键清理拒绝的Offer
        </el-button>
      </div>
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
        <el-table-column label="投递状态" width="120">
          <template #default="{ row }">
            <el-tag :type="applicationStatusTagType(row.status)">
              {{ applicationStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Offer状态" width="140">
          <template #default="{ row }">
            <template v-if="row.status === 5 && offerInfoMap[row.id]">
              <el-tag :type="offerStatusTagType(offerInfoMap[row.id].status)">
                {{ offerStatusText(offerInfoMap[row.id].status) }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="拒绝理由" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{
              row.status === 5 ? offerInfoMap[row.id]?.rejectReason || "-" : "-"
            }}
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="投递时间" min-width="170" />
        <el-table-column
          prop="remarks"
          label="备注"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 4">
              <el-button type="primary" link @click.stop="openDetail(row)">
                发送 Offer
              </el-button>
              <el-button
                type="danger"
                link
                :loading="rejectLoadingId === row.id"
                @click.stop="rejectAndNotify(row)"
              >
                淘汰并通知
              </el-button>
            </template>
            <span v-else>-</span>
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
      title="发送 Offer"
      width="780px"
      destroy-on-close
    >
      <template v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="投递ID">{{
            currentRow.id || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="求职者">{{
            currentRow.jobSeekerName || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="职位">{{
            currentRow.jobTitle || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="投递状态">
            <el-tag type="warning">面试中</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="投递时间">{{
            currentRow.applyTime || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="HR备注">{{
            currentRow.remarks || "-"
          }}</el-descriptions-item>
        </el-descriptions>

        <div class="form-wrap">
          <div class="section-title">Offer 信息</div>
          <el-form :model="offerForm" label-width="120px">
            <el-form-item label="预计入职日期">
              <el-date-picker
                v-model="offerForm.expectedEntryDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择预计入职日期"
                style="width: 220px"
              />
            </el-form-item>

            <el-form-item label="税前月薪">
              <el-input-number
                v-model="offerForm.salaryMonthly"
                :min="0"
                :precision="2"
                :step="500"
                controls-position="right"
                style="width: 220px"
              />
            </el-form-item>

            <el-form-item label="年薪月数">
              <el-input-number
                v-model="offerForm.salaryMonthCount"
                :min="12"
                :max="24"
                :step="1"
                controls-position="right"
                style="width: 220px"
              />
            </el-form-item>

            <el-form-item label="试用期(月)">
              <el-input-number
                v-model="offerForm.probationMonths"
                :min="0"
                :max="24"
                :step="1"
                controls-position="right"
                style="width: 220px"
              />
            </el-form-item>

            <el-form-item label="确认截止日期" required>
              <el-date-picker
                v-model="offerForm.offerDeadline"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择 Offer 截止日期"
                style="width: 220px"
              />
            </el-form-item>

            <el-form-item label="福利说明">
              <el-input
                v-model="offerForm.welfare"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="如五险一金、补贴、假期等"
              />
            </el-form-item>

            <el-form-item label="备注">
              <el-input
                v-model="offerForm.remark"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="补充说明"
              />
            </el-form-item>
          </el-form>
        </div>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitOffer">
          发送 Offer
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getJobApplicationList,
  reviewJobApplicationBatch,
} from "@/api/application";
import { pushSystemMessage } from "@/api/chat";
import { getSentOfferList, sendOffer } from "@/api/offer";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();

const quickFilter = ref("all");

const query = reactive({
  companyId: userStore?.companyId || undefined,
  statusList: [4, 5, 6],
  pageNum: 1,
  pageSize: 10,
});

const tableLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const offerInfoMap = ref({});

const detailVisible = ref(false);
const currentRow = ref(null);
const submitLoading = ref(false);
const cleanLoading = ref(false);
const rejectLoadingId = ref(null);

const offerForm = reactive({
  expectedEntryDate: "",
  salaryMonthly: null,
  salaryMonthCount: 12,
  probationMonths: 3,
  offerDeadline: "",
  welfare: "",
  remark: "",
});

const fetchList = async () => {
  if (!query.companyId) {
    ElMessage.warning("未获取到公司ID，请重新登录后重试");
    return;
  }

  tableLoading.value = true;
  try {
    const { data } = await getJobApplicationList({
      companyId: query.companyId,
      statusList: query.statusList,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    });

    const page = data?.data || data || {};
    tableData.value = page.records || page.list || [];
    total.value = page.total || 0;
    await loadOfferInfoForCurrentPage();
  } catch (e) {
    ElMessage.error(e?.message || "获取投递记录失败");
  } finally {
    tableLoading.value = false;
  }
};

const applicationStatusMap = {
  4: "面试中",
  5: "已发Offer",
  6: "已录用",
  7: "已淘汰",
};

const applicationStatusText = (status) =>
  applicationStatusMap[status] || `状态${status}`;

const applicationStatusTagType = (status) => {
  switch (status) {
    case 4:
      return "warning";
    case 5:
      return "success";
    case 6:
      return "success";
    case 7:
      return "danger";
    default:
      return "info";
  }
};

const offerStatusMap = {
  0: "待确认",
  1: "已接受",
  2: "已拒绝",
  3: "已过期",
  4: "已撤回",
  5: "已取消",
};

const offerStatusText = (status) => offerStatusMap[status] || `状态${status}`;

const offerStatusTagType = (status) => {
  switch (status) {
    case 0:
      return "warning";
    case 1:
      return "success";
    case 2:
      return "danger";
    default:
      return "info";
  }
};

const loadOfferInfoForCurrentPage = async () => {
  const targetRows = tableData.value.filter((item) => item.status === 5);
  if (targetRows.length === 0) {
    offerInfoMap.value = {};
    return;
  }

  try {
    const { data } = await getSentOfferList({
      recruiterId: userStore.userId,
      companyId: query.companyId,
      pageNum: 1,
      pageSize: 1000,
    });
    const page = data?.data || data || {};
    const list = page.records || page.list || [];
    const ids = new Set(targetRows.map((item) => item.id));
    const map = {};
    for (const offer of list) {
      if (!ids.has(offer.jobApplicationId)) {
        continue;
      }
      if (!map[offer.jobApplicationId]) {
        map[offer.jobApplicationId] = offer;
      }
    }
    offerInfoMap.value = map;
  } catch {
    offerInfoMap.value = {};
  }
};

const handleSizeChange = () => {
  query.pageNum = 1;
  fetchList();
};

const handleQuickFilterChange = (value) => {
  if (value === "interviewing") {
    query.statusList = [4];
  } else if (value === "offerSent") {
    query.statusList = [5];
  } else if (value === "hired") {
    query.statusList = [6];
  } else {
    query.statusList = [4, 5, 6];
  }
  query.pageNum = 1;
  fetchList();
};

const resetOfferForm = () => {
  offerForm.expectedEntryDate = "";
  offerForm.salaryMonthly = null;
  offerForm.salaryMonthCount = 12;
  offerForm.probationMonths = 3;
  offerForm.offerDeadline = "";
  offerForm.welfare = "";
  offerForm.remark = "";
};

const openDetail = (row) => {
  if (row?.status !== 4) {
    return;
  }
  currentRow.value = row;
  resetOfferForm();
  detailVisible.value = true;
};

const isPastDate = (dateText) => {
  if (!dateText) return false;
  const target = new Date(dateText);
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return target < now;
};

const submitOffer = async () => {
  if (!currentRow.value?.id) {
    ElMessage.error("投递记录ID不存在");
    return;
  }
  if (!offerForm.offerDeadline) {
    ElMessage.warning("请选择 Offer 截止日期");
    return;
  }
  if (isPastDate(offerForm.offerDeadline)) {
    ElMessage.warning("Offer 截止日期不能早于今天");
    return;
  }

  const jobSeekerId = currentRow.value.userId;
  const jobPositionId = currentRow.value.jobPositionId;
  if (!jobSeekerId || !jobPositionId) {
    ElMessage.error("投递记录字段不完整，缺少求职者ID或职位ID");
    return;
  }

  submitLoading.value = true;
  try {
    await sendOffer({
      jobApplicationId: currentRow.value.id,
      companyId: query.companyId,
      jobSeekerId,
      recruiterId: userStore.userId,
      jobPositionId,
      expectedEntryDate: offerForm.expectedEntryDate || null,
      salaryMonthly: offerForm.salaryMonthly,
      salaryMonthCount: offerForm.salaryMonthCount,
      probationMonths: offerForm.probationMonths,
      offerDeadline: offerForm.offerDeadline,
      welfare: offerForm.welfare?.trim() || "",
      remark: offerForm.remark?.trim() || "",
    });

    ElMessage.success("Offer 发送成功");
    detailVisible.value = false;
    fetchList();
  } catch (e) {
    ElMessage.error(e?.message || "发送 Offer 失败");
  } finally {
    submitLoading.value = false;
  }
};

const rejectAndNotify = async (row) => {
  const applicationId = row?.id;
  const jobSeekerId = row?.userId;
  if (!applicationId || !jobSeekerId) {
    ElMessage.error("投递记录字段不完整，无法执行淘汰操作");
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认将“${row.jobSeekerName || "该求职者"}”的投递标记为已淘汰，并发送通知消息？`,
      "确认淘汰",
      {
        type: "warning",
        confirmButtonText: "确认",
        cancelButtonText: "取消",
      },
    );
  } catch {
    return;
  }

  rejectLoadingId.value = applicationId;
  try {
    await reviewJobApplicationBatch({
      ids: [applicationId],
      status: 7,
      remarks: "面试后不合适，已淘汰",
    });

    const jobTitle = row?.jobTitle || "该职位";
    await pushSystemMessage({
      content: `您的投递（${jobTitle}）已更新为“已淘汰”。感谢您的参与，祝您求职顺利。`,
      sendToAll: false,
      targetUserIds: [jobSeekerId],
      includeSelf: false,
    });

    ElMessage.success("已淘汰并发送通知");
    await fetchList();
  } catch (e) {
    ElMessage.error(e?.message || "淘汰操作失败");
  } finally {
    rejectLoadingId.value = null;
  }
};

const cleanRejectedOffers = async () => {
  let ids = [];

  try {
    const [offerRes, applicationRes] = await Promise.all([
      getSentOfferList({
        recruiterId: userStore.userId,
        companyId: query.companyId,
        status: 2,
        pageNum: 1,
        pageSize: 1000,
      }),
      getJobApplicationList({
        companyId: query.companyId,
        status: 5,
        pageNum: 1,
        pageSize: 1000,
      }),
    ]);

    const offerPage = offerRes?.data?.data || offerRes?.data || {};
    const offerList = offerPage.records || offerPage.list || [];
    const rejectedIds = new Set(
      offerList.map((item) => item.jobApplicationId).filter(Boolean),
    );

    const appPage = applicationRes?.data?.data || applicationRes?.data || {};
    const appList = appPage.records || appPage.list || [];
    ids = appList.map((item) => item.id).filter((id) => rejectedIds.has(id));
  } catch (e) {
    ElMessage.error(e?.message || "获取待清理数据失败");
    return;
  }

  if (ids.length === 0) {
    ElMessage.info("没有可清理的拒绝Offer记录");
    return;
  }

  try {
    await ElMessageBox.confirm(
      `将批量把 ${ids.length} 条投递记录更新为“已淘汰”，确认继续？`,
      "一键清理拒绝Offer",
      {
        type: "warning",
        confirmButtonText: "确认清理",
        cancelButtonText: "取消",
      },
    );
  } catch {
    return;
  }

  cleanLoading.value = true;
  try {
    await reviewJobApplicationBatch({
      ids,
      status: 7,
      remarks: "拒绝Offer后批量清理",
    });
    ElMessage.success("清理完成");
    await fetchList();
  } catch (e) {
    ElMessage.error(e?.message || "清理失败");
  } finally {
    cleanLoading.value = false;
  }
};

onMounted(() => {
  fetchList();
});
</script>

<style lang="scss" scoped>
.send-offer {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .filter-card {
    .filter-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .filter-label {
      color: var(--el-text-color-secondary);
      font-size: 14px;
      white-space: nowrap;
    }

    :deep(.el-card__body) {
      padding: 12px;
    }
  }

  .tip-card {
    .tip-row {
      display: flex;
      align-items: center;
      gap: 12px;

      :deep(.el-alert) {
        flex: 1;
      }
    }

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

  .form-wrap {
    margin-top: 16px;

    .section-title {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 12px;
    }
  }
}
</style>
