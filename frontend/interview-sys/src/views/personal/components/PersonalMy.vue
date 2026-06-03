<template>
  <div class="my-page">
    <div v-if="!hasUserData" class="no-data-tip">暂无用户数据，请先登录</div>
    <el-descriptions title="用户信息" border>
      <el-descriptions-item
        :rowspan="2"
        :width="140"
        label="头像"
        align="center"
      >
        <el-image
          style="width: 100px; height: 100px"
          :src="url"
          :zoom-rate="1.2"
          :max-scale="7"
          :min-scale="0.2"
          :preview-src-list="srcList"
          show-progress
          fit="cover"
        >
          <template>
            <div class="image-slot viewer-error">
              <el-icon><icon-picture /></el-icon>
            </div>
          </template>
        </el-image>
      </el-descriptions-item>
      <el-descriptions-item label="用户名">{{
        userStore.username || "未设置"
      }}</el-descriptions-item>
      <el-descriptions-item label="账号状态">{{
        userStore.accountStatus || "未设置"
      }}</el-descriptions-item>
      <el-descriptions-item label="电话">{{
        userStore.phone || "未设置"
      }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{
        userStore.email || "未设置"
      }}</el-descriptions-item>
      <el-descriptions-item
        v-if="userStore.role !== 'JOB_SEEKER'"
        label="所属公司"
      >
        {{ userStore.companyName || "未设置" }}
      </el-descriptions-item>
      <el-descriptions-item label="个人简介">
        {{ userStore.profile || "暂无简介" }}
      </el-descriptions-item>
    </el-descriptions>
    <div id="info-edit-btn">
      <el-button
        type="primary"
        round
        :icon="Edit"
        @click="router.push('/personal/info/edit')"
        >编辑</el-button
      >
    </div>
    <el-container>
      <el-main>
        <el-container style="height: 100%">
          <el-header>
            <!-- <el-menu
              :default-active="4"
              mode="horizontal"
              class="horizontal-menu"
            >
              <el-menu-item index="1">沟通过</el-menu-item>
              <el-menu-item index="2">已投递</el-menu-item>
              <el-menu-item index="3">面试</el-menu-item>
              <el-menu-item index="4">感兴趣</el-menu-item>
            </el-menu> -->
          </el-header>
          <el-main><router-view /></el-main>
        </el-container>
      </el-main>
      <el-aside>
        <!-- <el-card
          style="height: 400px; width: 80%; margin: 0 auto; margin-top: 20px"
        >
          <p v-for="o in 4" :key="o" class="text item">
            {{ "List item " + o }}
          </p>
        </el-card> -->
      </el-aside>
    </el-container>
  </div>
</template>

<script setup>
import router from "@/router";
import { computed, onMounted } from "vue";
import { Edit } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();

const url = computed(() => userStore.avatarUrl || "");
const srcList = computed(() => (url.value ? [url.value] : []));

// 添加数据验证
const hasUserData = computed(() => {
  return userStore.username || userStore.email || userStore.phone;
});

// 组件挂载时检查数据
onMounted(() => {
  if (!hasUserData.value) {
    // 可以在这里触发数据获取逻辑
    console.log("用户数据未加载");
  }
});
</script>

<style lang="scss" scoped>
.my-page {
  .no-data-tip {
    text-align: center;
    padding: 20px;
    color: #909399;
  }
  .el-descriptions {
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
  }
  #info-edit-btn {
    width: 100%;
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  .el-container {
    flex: 1;
    .el-aside {
      border-left: 3px solid #eee;
    }
    .horizontal-menu {
      .el-menu-item {
        border-radius: 12px;
        margin: 0 10px;
        padding: 0 10px;
      }
    }
  }
}
</style>
