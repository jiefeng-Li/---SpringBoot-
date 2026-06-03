<template>
  <div class="join-interview">
    <el-card shadow="never" class="tip-card">
      <el-alert
        title="仅展示线上且已确认的面试记录，点击加入即可进入会议页面。"
        type="info"
        :closable="false"
      />
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="tableLoading" border stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="jobSeekerName" label="求职者" min-width="120" />
        <el-table-column prop="creatorName" label="发起人" min-width="120" />
        <el-table-column prop="rtcRoomId" label="房间号" min-width="120" />
        <el-table-column prop="rtcRoomName" label="会议名称" min-width="160" />
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
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" link @click="joinMeeting(row)">
              参加面试
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getInterviewNoticeList } from "@/api/interview";

const router = useRouter();

const query = reactive({
  status: 1,
  interviewType: 1,
  pageNum: 1,
  pageSize: 10,
});

const tableLoading = ref(false);
const tableData = ref([]);
const total = ref(0);

const fetchList = async () => {
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
    ElMessage.error(e?.message || "获取可参加面试列表失败");
  } finally {
    tableLoading.value = false;
  }
};

const handleSizeChange = () => {
  query.pageNum = 1;
  fetchList();
};

const joinMeeting = (row) => {
  if (!row?.id) {
    ElMessage.warning("面试记录不存在");
    return;
  }
  router.push(`/meeting/trtc?noticeId=${row.id}`);
};

onMounted(() => {
  fetchList();
});
</script>

<style scoped lang="scss">
.join-interview {
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
}
</style>
