package dev.haotangyuan.knownote.user.api;

import dev.haotangyuan.knownote.common.Result;
import dev.haotangyuan.knownote.user.api.dto.req.*;
import dev.haotangyuan.knownote.user.api.dto.resp.TokenRespDTO;
import dev.haotangyuan.knownote.user.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@Tag(name = "认证", description = "注册、登录、Token 刷新")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 发送验证码
     */
    @PostMapping("/code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeReqDTO req) {
        authService.sendCode(req);
        return Result.ok();
    }

    /**
     * 邮箱注册
     */
    @PostMapping("/register")
    public Result<TokenRespDTO> register(@Valid @RequestBody RegisterReqDTO req) {
        return Result.ok(authService.register(req));
    }

    /**
     * 密码登录
     */
    @PostMapping("/login/password")
    public Result<TokenRespDTO> loginByPassword(@Valid @RequestBody PasswordLoginReqDTO req) {
        return Result.ok(authService.loginByPassword(req));
    }

    /**
     * 验证码登录
     */
    @PostMapping("/login/code")
    public Result<TokenRespDTO> loginByCode(@Valid @RequestBody CodeLoginReqDTO req) {
        return Result.ok(authService.loginByCode(req));
    }

    /**
     * Google 登录（One Tap / ID Token 方式）
     */
    @PostMapping("/login/google")
    public Result<TokenRespDTO> loginByGoogle(@Valid @RequestBody GoogleLoginReqDTO req) {
        return Result.ok(authService.loginByGoogle(req));
    }

    /**
     * Google 登录（OAuth Callback / 授权码方式）
     */
    @PostMapping("/google/callback")
    public Result<TokenRespDTO> loginByGoogleCallback(@Valid @RequestBody GoogleCallbackReqDTO req) {
        return Result.ok(authService.loginByGoogleCallback(req));
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public Result<TokenRespDTO> refresh(@Valid @RequestBody RefreshReqDTO req) {
        return Result.ok(authService.refresh(req));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody LogoutReqDTO req) {
        authService.logout(req);
        return Result.ok();
    }
}
