<template>
  <el-scrollbar style="border-right: 0.025rem solid #f0f0f0">
    <el-menu class="menu-style" :active-index="activeIndex" router>
      <el-menu-item index="/personal/my">
        <el-icon><Avatar /></el-icon>
        <span>我的</span>
      </el-menu-item>
      <el-menu-item index="/personal/interview" v-if="role == 'JOB_SEEKER'">
        <el-icon><Calendar /></el-icon>
        <span>面试通知</span>
      </el-menu-item>
      <el-menu-item index="/personal/message">
        <el-icon><Message /></el-icon>
        <span>消息</span>
      </el-menu-item>
      <!-- <el-menu-item index="/personal/settings">
        <el-icon><Setting /></el-icon>
        <span>系统设置</span>
      </el-menu-item> -->
      <!-- <el-menu-item index="/personal/help">
        <el-icon><Help /></el-icon>
        <span>帮助中心</span>
      </el-menu-item> -->
    </el-menu>
  </el-scrollbar>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useUserStore } from "@/stores";
import { useRoute } from "vue-router";
import {
  Setting,
  Message,
  Avatar,
  Grid,
  Help,
  Calendar,
} from "@element-plus/icons-vue";

const userStore = useUserStore();
const role = ref(""); // 获取用户角色

const route = useRoute();
const activeIndex = computed(() => route.path);
onMounted(() => {
  // 获取用户角色
  role.value = userStore.role;
});
</script>

<style lang="scss" scoped>
.menu-style {
  border: 0;
}
</style>
