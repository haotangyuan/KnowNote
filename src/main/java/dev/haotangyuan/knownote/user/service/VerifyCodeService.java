package dev.haotangyuan.knownote.user.service;

import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private final StringRedisTemplate redisTemplate;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from:}")
    private String fromEmail;

    private static final String CODE_PREFIX = "verify_code:";
    private static final String COOLDOWN_PREFIX = "verify_code_cooldown:";
    private static final String DAILY_PREFIX = "verify_code_daily:";

    private static final int CODE_TTL_SECONDS = 300; // 5分钟
    private static final int COOLDOWN_SECONDS = 60; // 60秒冷却
    private static final int DAILY_LIMIT = 5; // 每天5次

    /**
     * 发送验证码
     */
    public void sendCode(String email) {
        // 检查冷却
        Long cooldownTtl = redisTemplate.getExpire(COOLDOWN_PREFIX + email, TimeUnit.SECONDS);
        if (cooldownTtl != null && cooldownTtl > 0) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "验证码发送过于频繁，请稍后再试");
        }

        // 检查日限额
        String dailyKey = DAILY_PREFIX + email;
        String dailyCountStr = redisTemplate.opsForValue().get(dailyKey);
        int dailyCount = 0;
        if (dailyCountStr != null) {
            try {
                dailyCount = Integer.parseInt(dailyCountStr);
            } catch (NumberFormatException e) {
                log.warn("邮箱 {} 的日次数格式错误，重置为0", email);
                redisTemplate.delete(dailyKey);
            }
        }
        if (dailyCount >= DAILY_LIMIT) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "今日验证码发送次数已达上限");
        }

        // 生成6位数字验证码
        String code = RandomUtil.randomNumbers(6);

        // 直接存储
        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);

        // 设置冷却
        redisTemplate.opsForValue().set(COOLDOWN_PREFIX + email, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        // 增加日计数（TTL 到当天24:00）
        long dailyCountTtl = Duration.between(LocalTime.now(), LocalTime.MAX).getSeconds();

        if (dailyCount == 0) {
            redisTemplate.opsForValue().set(dailyKey, "1", dailyCountTtl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(dailyKey);
            // 重新设置TTL确保到当天24:00过期
            redisTemplate.expire(dailyKey, dailyCountTtl, TimeUnit.SECONDS);
        }

        // 发送邮件
        sendEmail(email, code);
    }

    /**
     * 校验验证码
     */
    public boolean verifyCode(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (storedCode == null) {
            return false;
        }
        return storedCode.equals(code);
    }

    /**
     * 消费验证码（校验成功后删除）
     */
    public void consumeCode(String email) {
        redisTemplate.delete(CODE_PREFIX + email);
    }

    private void sendEmail(String email, String code) {
        if (StrUtil.isBlank(resendApiKey)) {
            log.warn("Resend API key 未配置，跳过发送邮件。验证码 for {}: {}", email, code);
            return;
        }

        try {
            String body = JSONUtil.toJsonStr(
                Map.of(
                    "from", fromEmail,
                    "to", email,
                    "subject", "KnowNote 验证码",
                    "html", StrUtil.format(
                        "<p>您的验证码是：<strong>{}</strong></p><p>有效期5分钟，请勿泄露。</p>",
                        code
                    )
                )
            );

            HttpUtil.createPost("https://api.resend.com/emails")
                .bearerAuth(resendApiKey)
                .body(body, "application/json")
                .execute();

            log.info("发送验证码成功，收件人：{}", email);
        } catch (Exception e) {
            log.error("发送邮件失败，收件人：{}", email, e);
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "邮件发送失败，请稍后重试");
        }
    }
}
