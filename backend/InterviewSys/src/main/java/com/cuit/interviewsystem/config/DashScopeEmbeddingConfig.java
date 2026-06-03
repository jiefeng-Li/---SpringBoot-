package com.cuit.interviewsystem.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashScopeEmbeddingConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Bean
    public DashScopeApi dashScopeApi() {
        return DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public EmbeddingModel dashScopeEmbeddingModel(DashScopeApi dashScopeApi) {
        return new DashScopeEmbeddingModel(
                dashScopeApi,
                MetadataMode.EMBED,
                DashScopeEmbeddingOptions.builder()
                        .withModel("text-embedding-v3")
                        .build()
        );
    }
}