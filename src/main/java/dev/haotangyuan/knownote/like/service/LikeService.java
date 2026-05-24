package dev.haotangyuan.knownote.like.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.hutool.core.util.IdUtil;
import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;
import dev.haotangyuan.knownote.common.UserContext;
import dev.haotangyuan.knownote.like.api.dto.req.LikePostReqDTO;
import dev.haotangyuan.knownote.like.api.dto.resp.LikeStatusRespDTO;
import dev.haotangyuan.knownote.like.domain.enums.LikeAction;
import dev.haotangyuan.knownote.like.mq.LikeDeltaMessage;
import dev.haotangyuan.knownote.like.mq.LikeDeltaProducer;
import dev.haotangyuan.knownote.post.domain.entity.PostDO;
import dev.haotangyuan.knownote.post.domain.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 点赞/收藏服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
public class LikeService {

    private final PostMapper postMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final LikeDeltaProducer likeDeltaProducer;

    private static final int SHARD_SIZE = 32768;
    private static final String LIKE_KEY_PREFIX = "plike:{}:{}"; // postId, shardId

    public Boolean likeAction(LikePostReqDTO req) {
        if (req.getAction() == LikeAction.LIKE) {
            return likePost(req.getPostId());
        } else if (req.getAction() == LikeAction.UNLIKE) {
            return unlikePost(req.getPostId());
        }
        return false;
    }

    /**
     * 点赞
     */
    public Boolean likePost(Long postId) {
        Long userId = UserContext.getUserId();
        long shardId = getShardId(userId);
        int offset = getOffset(userId);
        String key = StrUtil.format(LIKE_KEY_PREFIX, postId, shardId);
        Boolean oldBit = stringRedisTemplate.opsForValue().setBit(key, offset, true);
        boolean wasLiked = Boolean.TRUE.equals(oldBit);
        if (wasLiked) {
            return false;
        }
        try {
            Long creatorId = getCreatorId(postId);
            LikeDeltaMessage message = LikeDeltaMessage.builder()
                    .eventId(IdUtil.fastSimpleUUID())
                    .postId(postId)
                    .creatorId(creatorId)
                    .delta(1)
                    .createdAt(LocalDateTime.now())
                    .build();
            likeDeltaProducer.send(message);
            log.info("点赞成功: userId={}, postId={}", userId, postId);
            return true;
        } catch (BizException e) {
            stringRedisTemplate.opsForValue().setBit(key, offset, false);
            throw e;
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().setBit(key, offset, false);
            log.error("点赞失败: userId={}, postId={}", userId, postId, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "点赞失败，请稍后重试");
        }
    }

    public Boolean unlikePost(Long postId) {
        Long userId = UserContext.getUserId();
        long shardId = getShardId(userId);
        int offset = getOffset(userId);
        String key = StrUtil.format(LIKE_KEY_PREFIX, postId, shardId);
        Boolean oldBit = stringRedisTemplate.opsForValue().setBit(key, offset, false);
        boolean wasLiked = Boolean.TRUE.equals(oldBit);
        if (!wasLiked) {
            return false;
        }
        try {
            Long creatorId = getCreatorId(postId);
            LikeDeltaMessage message = LikeDeltaMessage.builder()
                    .eventId(IdUtil.fastSimpleUUID())
                    .postId(postId)
                    .creatorId(creatorId)
                    .delta(-1)
                    .createdAt(LocalDateTime.now())
                    .build();
            likeDeltaProducer.send(message);
            log.info("取消点赞成功: userId={}, postId={}", userId, postId);
            return true;
        } catch (BizException e) {
            stringRedisTemplate.opsForValue().setBit(key, offset, true);
            throw e;
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().setBit(key, offset, true);
            log.error("取消点赞失败: userId={}, postId={}", userId, postId, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "取消点赞失败，请稍后重试");
        }
    }

    /**
     * 查询单个帖子的点赞状态
     */
    public Boolean getStatus(Long postId) {
        Long userId = UserContext.getUserId();
        long shardId = getShardId(userId);
        int offset = getOffset(userId);
        String key = StrUtil.format(LIKE_KEY_PREFIX, postId, shardId);
        return stringRedisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 批量查询多个帖子的点赞状态
     */
    public List<LikeStatusRespDTO> batchGetStatus(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        Long userId = UserContext.getUserId();
        long shardId = getShardId(userId);
        int offset = getOffset(userId);

        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long postId : postIds) {
                String key = StrUtil.format(LIKE_KEY_PREFIX, postId, shardId);
                connection.stringCommands().getBit(key.getBytes(), offset);
            }
            return null;
        });

        List<LikeStatusRespDTO> statusList = new ArrayList<>(postIds.size());
        for (int i = 0; i < postIds.size(); i++) {
            boolean liked = Boolean.TRUE.equals(results.get(i));
            statusList.add(new LikeStatusRespDTO(postIds.get(i), liked));
        }
        return statusList;
    }

    /**
     * 计算分片 ID
     */
    private long getShardId(Long id) {
        return id / SHARD_SIZE;
    }

    /**
     * 计算分片内偏移
     */
    private int getOffset(Long id) {
        return (int) (id % SHARD_SIZE);
    }

    private Long getCreatorId(Long postId) {
        // TODO: 可考虑缓存 creatorId，减少 DB 查询
        LambdaQueryWrapper<PostDO> queryWrapper = Wrappers.lambdaQuery(PostDO.class)
                .eq(PostDO::getId, postId)
                .select(PostDO::getCreatorId);
        PostDO post = postMapper.selectOne(queryWrapper);
        if (post == null || post.getCreatorId() == null) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "帖子不存在");
        }
        return post.getCreatorId();
    }
}
