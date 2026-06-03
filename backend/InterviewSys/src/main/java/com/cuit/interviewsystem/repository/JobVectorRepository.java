package com.cuit.interviewsystem.repository;

import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JobVectorRepository {

    // 向量维度常量，定义为1024维
    private static final int VECTOR_DIMENSION = 1024;

    // 用于插入或更新职位嵌入数据的SQL语句
    private static final String UPSERT_SQL = """
            INSERT INTO t_job_embedding (job_id, company_id, job_title, content, embedding, embedding_model, updated_at)
            VALUES (?, ?, ?, ?, CAST(? AS vector(1024)), ?, NOW())
            ON CONFLICT (job_id) DO UPDATE SET
                company_id = EXCLUDED.company_id,
                job_title = EXCLUDED.job_title,
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                embedding_model = EXCLUDED.embedding_model,
                updated_at = NOW()
            """;

    // 用于删除指定job_id的职位嵌入数据的SQL语句
    private static final String DELETE_SQL = "DELETE FROM t_job_embedding WHERE job_id = ?";

    // 用于查询最相似职位ID的SQL语句
    private static final String QUERY_SQL = """
            SELECT job_id
            FROM t_job_embedding
            ORDER BY embedding <=> CAST(? AS vector(1024))
            LIMIT ?
            """;

    // 使用名为"recommendationJdbcTemplate"的JdbcTemplate实例
    @Resource(name = "recommendationJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * 插入或更新职位嵌入数据
     * @param jobId 职位ID
     * @param companyId 公司ID
     * @param jobTitle 职位标题
     * @param content 职位内容
     * @param embeddingLiteral 嵌入数据的字符串表示
     * @param embeddingModel 使用的嵌入模型名称
     */
    public void upsert(Long jobId, Long companyId, String jobTitle, String content, String embeddingLiteral, String embeddingModel) {
        jdbcTemplate.update(UPSERT_SQL, jobId, companyId, jobTitle, content, embeddingLiteral, embeddingModel);
    }

    /**
     * 删除指定job_id的职位嵌入数据
     * @param jobId 要删除的职位ID
     */
    public void deleteByJobId(Long jobId) {
        jdbcTemplate.update(DELETE_SQL, jobId);
    }

    /**
     * 查找与给定嵌入最相似的职位ID列表
     * @param embeddingLiteral 用于比较的嵌入数据的字符串表示
     * @param limit 返回的结果数量限制
     * @return 相似职位ID列表
     */
    public List<Long> findTopJobIdsByEmbedding(String embeddingLiteral, int limit) {
        // 使用RowMapper将结果集中的job_id列映射为Long类型
        RowMapper<Long> rowMapper = (rs, rowNum) -> rs.getLong("job_id");
        // 执行SQL查询，传入嵌入数据和限制数量参数，返回相似职位ID列表
        return jdbcTemplate.query(QUERY_SQL, rowMapper, embeddingLiteral, limit);
    }

    /**
     * 获取向量维度
     * @return 向量维度值
     */
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }
}