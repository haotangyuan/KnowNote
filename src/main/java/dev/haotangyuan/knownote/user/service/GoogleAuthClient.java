package dev.haotangyuan.knownote.user.service;

import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Google OAuth 客户端
 */
@Slf4j
@Component
public class GoogleAuthClient {

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    @Value("${google.redirect-uri:}")
    private String redirectUri;

    private GoogleIdTokenVerifier idTokenVerifier;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @PostConstruct
    public void init() {
        if (clientId != null && !clientId.isBlank()) {
            idTokenVerifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(List.of(clientId))
                    .build();
        }
    }

    /**
     * 验证 ID Token（One Tap 方式，本地验证）
     */
    public GoogleUserInfo verifyIdToken(String idTokenString) {
        if (idTokenVerifier == null) {
            throw new BizException(ErrorCode.SERVER_ERROR, "Google 登录未配置");
        }

        try {
            GoogleIdToken idToken = idTokenVerifier.verify(idTokenString);
            if (idToken == null) {
                throw new BizException(ErrorCode.CLIENT_ERROR, "Google 认证失败，请重试");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name")
            );
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google ID Token 验证失败", e);
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "Google 认证失败，请重试");
        }
    }

    /**
     * 用授权码换取 access_token（OAuth Callback 方式）
     */
    public String exchangeToken(String code) {
        try {
            HttpResponse resp = HttpRequest.post(TOKEN_URL)
                    .form("code", code)
                    .form("client_id", clientId)
                    .form("client_secret", clientSecret)
                    .form("redirect_uri", redirectUri)
                    .form("grant_type", "authorization_code")
                    .execute();

            if (!resp.isOk()) {
                log.error("Google token 交换失败: {}", resp.body());
                throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "Google 认证失败，请重试");
            }
            return JSONUtil.parseObj(resp.body()).getStr("access_token");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token 交换异常", e);
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "Google 认证失败，请重试");
        }
    }

    /**
     * 用 access_token 获取用户信息
     */
    public GoogleUserInfo fetchUserInfo(String accessToken) {
        try {
            HttpResponse resp = HttpRequest.get(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .execute();

            if (!resp.isOk()) {
                log.error("获取 Google 用户信息失败: {}", resp.body());
                throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "Google 认证失败，请重试");
            }

            var json = JSONUtil.parseObj(resp.body());
            return new GoogleUserInfo(
                    json.getStr("sub"),
                    json.getStr("email"),
                    json.getStr("name")
            );
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取 Google 用户信息异常", e);
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "Google 认证失败，请重试");
        }
    }

    public record GoogleUserInfo(String googleId, String email, String name) {}
}
