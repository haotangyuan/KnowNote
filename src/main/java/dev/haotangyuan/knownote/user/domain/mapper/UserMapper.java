package dev.haotangyuan.knownote.user.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.haotangyuan.knownote.user.domain.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

}
