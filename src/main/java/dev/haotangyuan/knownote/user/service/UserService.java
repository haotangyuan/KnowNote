package dev.haotangyuan.knownote.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;
import dev.haotangyuan.knownote.common.PasswordEncoder;
import dev.haotangyuan.knownote.common.UserContext;
import dev.haotangyuan.knownote.user.api.dto.req.GoogleLoginReqDTO;
import dev.haotangyuan.knownote.user.api.dto.req.UpdatePasswordReqDTO;
import dev.haotangyuan.knownote.user.api.dto.req.UpdateProfileReqDTO;
import dev.haotangyuan.knownote.user.api.dto.resp.UserMeRespDTO;
import dev.haotangyuan.knownote.user.domain.entity.UserDO;
import dev.haotangyuan.knownote.user.domain.mapper.UserMapper;
import dev.haotangyuan.knownote.user.service.GoogleAuthClient.GoogleUserInfo;

import cn.hutool.core.util.StrUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthClient googleAuthClient;

    public UserDO getById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id))
                .orElseThrow(() -> new BizException(ErrorCode.CLIENT_ERROR, "用户不存在"));
    }

    public UserDO getByUsername(String username) {
        return Optional.ofNullable(userMapper.selectOne(
                        new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username)))
                .orElseThrow(() -> new BizException(ErrorCode.CLIENT_ERROR, "用户不存在"));
    }

    /**
     * 获取当前用户信息
     */
    public UserMeRespDTO getMe() {
        Long userId = UserContext.getUserId();
        UserDO user = getById(userId);

        return UserMeRespDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .hasPassword(user.getPasswordHash() != null)
                .hasGoogle(user.getGoogleId() != null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 更新个人资料
     */
    public void updateProfile(UpdateProfileReqDTO req) {
        Long userId = UserContext.getUserId();
        UserDO user = getById(userId);

        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatar() != null) {
            user.setAvatar(req.getAvatar());
        }
        if (req.getBio() != null) {
            user.setBio(req.getBio());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    public void updatePassword(UpdatePasswordReqDTO req) {
        Long userId = UserContext.getUserId();
        UserDO user = getById(userId);

        if (user.getPasswordHash() != null) {
            if (StrUtil.isBlank(req.getOldPassword())) {
                throw new BizException(ErrorCode.CLIENT_ERROR, "请输入原密码");
            }
            if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
                throw new BizException(ErrorCode.CLIENT_ERROR, "原密码错误");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 绑定 Google 账号
     */
    public void bindGoogle(GoogleLoginReqDTO req) {
        Long userId = UserContext.getUserId();
        UserDO user = getById(userId);

        if (user.getGoogleId() != null) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "当前账号已绑定 Google");
        }

        GoogleUserInfo googleUser = googleAuthClient.verifyIdToken(req.getIdToken());

        UserDO existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getGoogleId, googleUser.googleId()));
        if (existingUser != null) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "该 Google 账号已绑定其他用户");
        }

        user.setGoogleId(googleUser.googleId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
