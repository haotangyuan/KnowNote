package dev.haotangyuan.knownote.research.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.knownote.research.domain.entity.WorkflowEventDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流事件 Mapper
 */
@Mapper
public interface WorkflowEventMapper extends BaseMapper<WorkflowEventDO> {
}
