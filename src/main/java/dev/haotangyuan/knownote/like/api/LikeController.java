package dev.haotangyuan.knownote.like.api;

import dev.haotangyuan.knownote.common.Result;
import dev.haotangyuan.knownote.like.api.dto.req.LikePostReqDTO;
import dev.haotangyuan.knownote.like.api.dto.resp.LikeStatusRespDTO;
import dev.haotangyuan.knownote.like.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 点赞控制器
 */
@RestController
@RequestMapping("/api/v1/like")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
public class LikeController {

    private final LikeService likeService;

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/post")
    public Result<Boolean> likeAction(@Valid @RequestBody LikePostReqDTO req) {
        return Result.ok(likeService.likeAction(req));
    }

    /**
     * 批量查询点赞状态
     */
    @GetMapping("/status")
    public Result<List<LikeStatusRespDTO>> getStatus(@RequestParam List<Long> postIds) {
        return Result.ok(likeService.batchGetStatus(postIds));
    }
}
