package dev.haotangyuan.knownote.research.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.knownote.research.domain.entity.ResearchSessionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 研究会话 Mapper
 */
@Mapper
public interface ResearchSessionMapper extends BaseMapper<ResearchSessionDO> {

    @Select("SELECT * FROM research_session WHERE id = #{id} FOR UPDATE")
    ResearchSessionDO selectByIdForUpdate(@Param("id") String id);

    @Update("""
            <script>
            UPDATE research_session
            SET status = #{status},
                update_time = NOW()
                <if test="setStartTime">, start_time = NOW()</if>
                <if test="setCompleteTime">, complete_time = NOW()</if>
                , total_input_tokens = #{inputTokens}
                , total_output_tokens = #{outputTokens}
            WHERE id = #{id}
            </script>
            """)
    void updateSession(@Param("id") String id, @Param("status") String status,
                       @Param("setStartTime") boolean setStartTime, @Param("setCompleteTime") boolean setCompleteTime,
                       @Param("inputTokens") long inputTokens, @Param("outputTokens") long outputTokens);

    @Update("""
            UPDATE research_session
            SET status = 'QUEUE', update_time = NOW()
            WHERE id = #{id} AND user_id = #{userId}
              AND status IN ('NEW', 'NEED_CLARIFICATION')
            """)
    int casUpdateToQueue(@Param("id") String id, @Param("userId") Long userId);

    @Update("""
            <script>
            UPDATE research_session
            SET update_time = NOW()
            <if test="modelId != null">, model_id = #{modelId}</if>
            <if test="budget != null">, budget = #{budget}</if>
            <if test="title != null">, title = #{title}</if>
            WHERE id = #{id} AND model_id IS NULL
            </script>
            """)
    int setInfoIfNull(@Param("id") String id, @Param("modelId") String modelId,
                                 @Param("budget") String budget, @Param("title") String title);

    @Select("""
            SELECT COUNT(*) FROM research_session 
            WHERE model_id = #{modelId} 
            AND status NOT IN ('COMPLETED', 'FAILED')
            """)
    int countActiveUsage(@Param("modelId") String modelId);
}
