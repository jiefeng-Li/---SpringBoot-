<template>
  <el-scrollbar>
    <el-menu
      :default-active="activeMenu"
      class="el-menu-vertical-demo"
      @open="handleOpen"
      @close="handleClose"
      router
    >
      <!-- 职位管理模块 -->
      <el-sub-menu index="1">
        <template #title>
          <el-icon><Briefcase /></el-icon>
          <span>职位管理</span>
        </template>
        <el-menu-item
          index="/recruiter/job-publish"
          :disabled="!isCompanyBound"
        >
          发布职位
        </el-menu-item>
        <el-menu-item index="/recruiter/job-list" :disabled="!isCompanyBound">
          职位列表
        </el-menu-item>
      </el-sub-menu>

      <!-- 投递记录模块 -->
      <el-menu-item
        index="/recruiter/application-records"
        :disabled="!isCompanyBound"
      >
        <el-icon><Document /></el-icon>
        <span>投递记录</span>
      </el-menu-item>

      <!-- 面试管理模块 -->
      <el-sub-menu index="3">
        <template #title>
          <el-icon><ChatDotRound /></el-icon>
          <span>面试管理</span>
        </template>
        <el-menu-item
          index="/recruiter/interview-records"
          :disabled="!isCompanyBound"
        >
          面试记录
        </el-menu-item>
        <el-menu-item
          index="/recruiter/invite-interview"
          :disabled="!isCompanyBound"
        >
          邀约面试
        </el-menu-item>
        <el-menu-item
          index="/recruiter/join-interview"
          :disabled="!isCompanyBound"
        >
          参加面试
        </el-menu-item>
        <el-menu-item index="/recruiter/send-offer" :disabled="!isCompanyBound">
          发送Offer
        </el-menu-item>
      </el-sub-menu>
      <!-- 在设置模块前添加 -->
      <el-menu-item
        :index="
          isCompanyBound
            ? '/recruiter/unbind-company'
            : '/recruiter/bind-company'
        "
      >
        <el-icon><Link /></el-icon>
        <span>绑定公司</span>
      </el-menu-item>

      <!-- 设置模块 -->
      <!-- <el-menu-item index="/recruiter/settings">
        <el-icon><Setting /></el-icon>
        <span>设置</span>
      </el-menu-item> -->
    </el-menu>
  </el-scrollbar>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useUserStore } from "@/stores/user";
import {
  Briefcase,
  Document,
  ChatDotRound,
  Setting,
  Link,
} from "@element-plus/icons-vue";

const route = useRoute();
const userStore = useUserStore();

// 判断是否已绑定公司
const isCompanyBound = computed(() => {
  return !!userStore.companyId;
});

// 当前激活的菜单项
const activeMenu = computed(() => {
  return route.path;
});

const handleOpen = (key) => {
  console.log("Submenu opened:", key);
};

const handleClose = (key) => {
  console.log("Submenu closed:", key);
};
</script>

<style lang="scss" scoped>
.el-menu-vertical-demo {
  border-right: none;
}
</style>
