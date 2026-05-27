package dev.haotangyuan.knownote.studio.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.knownote.studio.domain.entity.StudioProjectDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudioProjectMapper extends BaseMapper<StudioProjectDO> {
}
