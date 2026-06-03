import request from "@/utils/request";

export const handleMockInterview = (data) => {
  return request({
    url: "/ai/mock-interview",
    method: "post",
    timeout: 120000,
    data,
  });
};