package dev.haotangyuan.knownote.storage.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.haotangyuan.knownote.common.Result;
import dev.haotangyuan.knownote.storage.api.dto.req.UploadUrlReqDTO;
import dev.haotangyuan.knownote.storage.api.dto.resp.UploadUrlRespDTO;
import dev.haotangyuan.knownote.storage.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 存储接口
 */
@Tag(name = "存储", description = "获取预签名上传 URL")
@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("not '${oss.endpoint:}'.blank")
@RequestMapping("/api/v1/oss")
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/url")
    public Result<UploadUrlRespDTO> getUploadUrl(@Valid @RequestBody UploadUrlReqDTO req) {
        return Result.ok(storageService.getUploadUrl(req));
    }
}
