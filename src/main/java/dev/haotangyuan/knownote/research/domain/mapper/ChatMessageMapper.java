package dev.haotangyuan.knownote.research.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.knownote.research.domain.entity.ChatMessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageDO> {

    @Select("""
            SELECT COALESCE(MAX(sequence_no), 0) FROM (
                SELECT sequence_no FROM chat_message WHERE research_id = #{researchId}
                UNION ALL
                SELECT sequence_no FROM workflow_event WHERE research_id = #{researchId}
            ) t
            """)
    Integer selectMaxSequenceByResearchId(@Param("researchId") String researchId);
}
