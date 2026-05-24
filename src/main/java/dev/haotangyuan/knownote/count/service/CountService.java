package dev.haotangyuan.knownote.count.service;

import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.StrUtil;
import dev.haotangyuan.knownote.count.domain.entity.PostCountDO;
import dev.haotangyuan.knownote.count.domain.entity.UserCountDO;
import dev.haotangyuan.knownote.count.domain.enums.PostCountField;
import dev.haotangyuan.knownote.count.domain.enums.UserCountField;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 计数服务
 */
@Service
@RequiredArgsConstructor
public class CountService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String POST_COUNT_KEY_PREFIX = "pcnt:{}";
    private static final String USER_COUNT_KEY_PREFIX = "ucnt:{}";
    private static final int POST_COUNT_SIZE = 8;
    private static final int USER_COUNT_SIZE = 20;

    private DefaultRedisScript<Long> incrCountScript;

    @PostConstruct
    public void init() {
        incrCountScript = new DefaultRedisScript<>();
        incrCountScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/incr_count.lua")));
        incrCountScript.setResultType(Long.class);
    }

    public PostCountDO getPostCount(Long postId) {
        String key = StrUtil.format(POST_COUNT_KEY_PREFIX, postId);
        byte[] value = getRawValue(key);
        return deserializePostCount(value);
    }

    public UserCountDO getUserCount(Long userId) {
        String key = StrUtil.format(USER_COUNT_KEY_PREFIX, userId);
        byte[] value = getRawValue(key);
        return deserializeUserCount(value);
    }

    public void incrPostCount(Long postId, PostCountField field, int delta) {
        String key = StrUtil.format(POST_COUNT_KEY_PREFIX, postId);
        stringRedisTemplate.execute(
                incrCountScript,
                Collections.singletonList(key),
                String.valueOf(field.getOffset()),
                String.valueOf(delta),
                String.valueOf(POST_COUNT_SIZE)
        );
    }

    public void incrUserCount(Long userId, UserCountField field, int delta) {
        String key = StrUtil.format(USER_COUNT_KEY_PREFIX, userId);
        stringRedisTemplate.execute(
                incrCountScript,
                Collections.singletonList(key),
                String.valueOf(field.getOffset()),
                String.valueOf(delta),
                String.valueOf(USER_COUNT_SIZE)
        );
    }

    private PostCountDO deserializePostCount(byte[] countBytes) {
        if (countBytes == null || countBytes.length < POST_COUNT_SIZE) {
            return new PostCountDO(0, 0);
        }
        int likeCount = ByteUtil.bytesToInt(countBytes, PostCountField.LIKED_COUNT.getOffset(), ByteOrder.BIG_ENDIAN);
        int favCount = ByteUtil.bytesToInt(countBytes, PostCountField.FAVED_COUNT.getOffset(), ByteOrder.BIG_ENDIAN);
        return new PostCountDO(likeCount, favCount);
    }

    private byte[] serializePostCount(PostCountDO postCountDO) {
        ByteBuffer buffer = ByteBuffer.allocate(POST_COUNT_SIZE).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(postCountDO.getLikedCount());
        buffer.putInt(postCountDO.getFavedCount());
        return buffer.array();
    }

    private UserCountDO deserializeUserCount(byte[] countBytes) {
        if (countBytes == null || countBytes.length < USER_COUNT_SIZE) {
            return new UserCountDO(0, 0, 0, 0, 0);
        }
        int following = ByteUtil.bytesToInt(countBytes, UserCountField.FOLLOWING.getOffset(), ByteOrder.BIG_ENDIAN);
        int follower = ByteUtil.bytesToInt(countBytes, UserCountField.FOLLOWER.getOffset(), ByteOrder.BIG_ENDIAN);
        int post = ByteUtil.bytesToInt(countBytes, UserCountField.POST.getOffset(), ByteOrder.BIG_ENDIAN);
        int liked = ByteUtil.bytesToInt(countBytes, UserCountField.LIKED.getOffset(), ByteOrder.BIG_ENDIAN);
        int faved = ByteUtil.bytesToInt(countBytes, UserCountField.FAVED.getOffset(), ByteOrder.BIG_ENDIAN);
        return new UserCountDO(following, follower, post, liked, faved);
    }

    private byte[] getRawValue(String key) {
        return stringRedisTemplate.execute((RedisCallback<byte[]>) connection ->
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] serializeUserCount(UserCountDO userCountDO) {
        ByteBuffer buffer = ByteBuffer.allocate(USER_COUNT_SIZE).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(userCountDO.getFollowing());
        buffer.putInt(userCountDO.getFollower());
        buffer.putInt(userCountDO.getPost());
        buffer.putInt(userCountDO.getLiked());
        buffer.putInt(userCountDO.getFaved());
        return buffer.array();
    }
}
