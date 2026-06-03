<template>
  <UIKitProvider theme="light" language="zh-CN">
    <ConferenceMainView v-if="isPC"></ConferenceMainView>
    <ConferenceMainViewH5 v-else></ConferenceMainViewH5>
  </UIKitProvider>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { UIKitProvider } from "@tencentcloud/uikit-base-component-vue3";
import {
  ConferenceMainView,
  ConferenceMainViewH5,
  conference,
} from "@tencentcloud/roomkit-web-vue3";
import { getPlatform } from "@tencentcloud/universal-api";
import { useRoute } from "vue-router";
import { getTrtcSig } from "@/api/trtc";

const isPC = ref(getPlatform() === "pc");

const route = useRoute();

const sdkAppId = ref(0);
const userId = ref(0);
const userSig = ref("");
const roomId = ref(null);
const roomName = ref("在线面试");

const init = async (noticeId) => {
  const data = await getTrtcSig(noticeId);
  console.log("获取TRTC签名成功：", data.data.data);
  sdkAppId.value = data.data.data.sdkAppId;
  userId.value = data.data.data.userId;
  userSig.value = data.data.data.userSig;
  roomId.value = data.data.data.roomId;
  roomName.value = data.data.data.roomName;
  console.log("sdkAppId:", sdkAppId.value);
  console.log("userId:", userId.value);
  console.log("userSig:", userSig.value);
  console.log("roomId:", roomId.value);
  console.log("roomName:", roomName.value);
};

onMounted(async () => {
  try {
    const noticeId = route.query.noticeId;
    await init(noticeId);
    await conference.login({
      sdkAppId: sdkAppId.value,
      userId: userId.value,
      userSig: userSig.value,
    });

    await conference.createAndJoinRoom({
      roomId: roomId.value,
      options: {
        roomName: roomName.value,
      },
    });
  } catch (error) {
    console.error("初始化失败：", error);
  }
});
</script>

<style>
html,
body {
  padding: 0 !important;
  margin: 0 !important;
}

#app {
  width: 100% !important;
  height: 100% !important;
  padding: 0 !important;
  margin: 0 !important;
  max-width: 100% !important;
  max-height: 100% !important;
  text-align: left !important;
  overflow: hidden;
}
</style>
