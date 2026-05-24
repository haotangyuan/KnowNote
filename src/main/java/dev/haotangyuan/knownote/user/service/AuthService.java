package dev.haotangyuan.knownote.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;
import dev.haotangyuan.knownote.common.PasswordEncoder;
import dev.haotangyuan.knownote.common.UserContext;
import dev.haotangyuan.knownote.user.api.dto.req.*;
import dev.haotangyuan.knownote.user.api.dto.resp.TokenRespDTO;
import dev.haotangyuan.knownote.user.domain.entity.UserDO;
import dev.haotangyuan.knownote.user.domain.mapper.UserMapper;
import dev.haotangyuan.knownote.user.service.GoogleAuthClient.GoogleUserInfo;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final VerifyCodeService verifyCodeService;
    private final TokenService tokenService;
    private final GoogleAuthClient googleAuthClient;

    private static final String AVATAR_TEMPLATE = "https://api.dicebear.com/9.x/pixel-art/svg?seed={}";

    /**
     * 发送验证码
     */
    public void sendCode(SendCodeReqDTO req) {
        verifyCodeService.sendCode(req.getEmail());
    }

    /**
     * 邮箱注册
     */
    @Transactional
    public TokenRespDTO register(RegisterReqDTO req) {
        if (!verifyCodeService.verifyCode(req.getEmail(), req.getCode())) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "验证码错误或已过期");
        }

        if (userMapper.selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getEmail, req.getEmail())) != null) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "邮箱已被注册");
        }

        if (userMapper.selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, req.getUsername())) != null) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "用户名已被使用");
        }

        LocalDateTime now = LocalDateTime.now();
        UserDO user = UserDO.builder()
                .email(req.getEmail())
                .username(req.getUsername())
                .nickname(req.getNickname())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .avatar(StrUtil.format(AVATAR_TEMPLATE, req.getUsername()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        userMapper.insert(user);
        verifyCodeService.consumeCode(req.getEmail());

        return tokenService.issueTokenPair(user.getId(), null);
    }

    /**
     * 密码登录
     */
    public TokenRespDTO loginByPassword(PasswordLoginReqDTO req) {
        UserDO user = Validator.isEmail(req.getAccount())
                ? userMapper.selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getEmail, req.getAccount()))
                : userMapper.selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, req.getAccount()));

        if (user == null) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "账号或密码错误");
        }

        if (user.getPasswordHash() == null) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "该账号未设置密码，请使用验证码或 Google 登录");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "账号或密码错误");
        }

        return tokenService.issueTokenPair(user.getId(), null);
    }

    /**
     * 验证码登录
     */
    public TokenRespDTO loginByCode(CodeLoginReqDTO req) {
        if (!verifyCodeService.verifyCode(req.getEmail(), req.getCode())) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "验证码错误或已过期");
        }

        UserDO user = userMapper.selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getEmail, req.getEmail()));
        if (user == null) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "该邮箱未注册");
        }

        verifyCodeService.consumeCode(req.getEmail());
        return tokenService.issueTokenPair(user.getId(), null);
    }

    /**
     * Google 登录（One Tap / ID Token 方式）
     */
    @Transactional
    public TokenRespDTO loginByGoogle(GoogleLoginReqDTO req) {
        GoogleUserInfo googleUser = googleAuthClient.verifyIdToken(req.getIdToken());
        return findOrCreateGoogleUser(googleUser);
    }

    /**
     * Google 登录（OAuth Callback / 授权码方式）
     */
    @Transactional
    public TokenRespDTO loginByGoogleCallback(GoogleCallbackReqDTO req) {
        String accessToken = googleAuthClient.exchangeToken(req.getCode());
        GoogleUserInfo googleUser = googleAuthClient.fetchUserInfo(accessToken);
        return findOrCreateGoogleUser(googleUser);
    }

    /**
     * 刷新 Token
     */
    public TokenRespDTO refresh(RefreshReqDTO req) {
        return tokenService.refresh(req.getRefreshToken());
    }

    /**
     * 登出
     */
    public void logout(LogoutReqDTO req) {
        Long userId = UserContext.getUserId();
        tokenService.logout(userId, req.getRefreshToken());
    }

    private TokenRespDTO findOrCreateGoogleUser(GoogleUserInfo googleUser) {
        // 1. 先用 googleId 查找
        UserDO userByGoogleId = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getGoogleId, googleUser.googleId()));
        if (userByGoogleId != null) {
            return tokenService.issueTokenPair(userByGoogleId.getId(), null);
        }

        // 2. 用 email 查找，找到则绑定 googleId
        UserDO userByEmail = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getEmail, googleUser.email()));
        if (userByEmail != null) {
            userByEmail.setGoogleId(googleUser.googleId());
            userByEmail.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(userByEmail);
            return tokenService.issueTokenPair(userByEmail.getId(), null);
        }

        // 3. 都没有，创建新用户
        String nickname = StrUtil.blankToDefault(googleUser.name(), null);
        LocalDateTime now = LocalDateTime.now();

        int attempts = 0;
        while (attempts < 10) {
            String randomUsername = "user_" + RandomUtil.randomString(8);
            String finalNickname = StrUtil.blankToDefault(nickname, randomUsername);

            UserDO user = UserDO.builder()
                    .email(googleUser.email())
                    .username(randomUsername)
                    .nickname(finalNickname)
                    .googleId(googleUser.googleId())
                    .avatar(StrUtil.format(AVATAR_TEMPLATE, randomUsername))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            try {
                userMapper.insert(user);
                return tokenService.issueTokenPair(user.getId(), null);
            } catch (DuplicateKeyException e) {
                attempts++;
                log.warn("用户名冲突，重试第{}次: {}", attempts, randomUsername);
            }
        }

        throw new BizException(ErrorCode.SERVER_ERROR, "生成用户名失败，请重试");
    }
}
