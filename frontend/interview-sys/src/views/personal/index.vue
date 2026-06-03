<template>
  <BaseLayout
    aside-width="400px"
    aside-background="#ffffff"
    main-background="#ffffff"
  >
    <template #header>
      <HomeHeader/>
    </template>
    <template #aside>
      <PersonalAside />
    </template>
    <template #main>
      <router-view></router-view>
    </template>
  </BaseLayout>
</template>

<script setup>
import BaseLayout from "../layout/index.vue";
import PersonalAside from "./components/PersonalAside.vue";
import HomeHeader from "@/views/home/components/HomeHeader.vue";
</script>

<style lang="scss" scoped></style>
