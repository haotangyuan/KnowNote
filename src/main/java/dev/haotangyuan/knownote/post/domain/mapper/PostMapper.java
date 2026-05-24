package dev.haotangyuan.knownote.post.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.knownote.post.domain.entity.PostDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子 Mapper
 */
@Mapper
public interface PostMapper extends BaseMapper<PostDO> {

}
