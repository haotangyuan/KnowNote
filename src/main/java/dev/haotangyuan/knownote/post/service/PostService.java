package dev.haotangyuan.knownote.post.service;

import dev.haotangyuan.knownote.post.api.dto.req.RollbackVersionReqDTO;
import dev.haotangyuan.knownote.post.api.dto.req.SavePostContentReqDTO;
import dev.haotangyuan.knownote.post.api.dto.req.SavePostMetadataReqDTO;
import dev.haotangyuan.knownote.post.api.dto.resp.CreatePostRespDTO;
import dev.haotangyuan.knownote.post.api.dto.resp.PostRespDTO;
import dev.haotangyuan.knownote.post.api.dto.resp.PostVersionRespDTO;
import dev.haotangyuan.knownote.post.domain.entity.PostDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 帖子服务接口
 */
public interface PostService {

    /**
     * 创建帖子
     */
    CreatePostRespDTO createPost();

    /**
     * 保存帖子内容
     */
    void saveContent(Long postId, SavePostContentReqDTO req);

    /**
     * 保存帖子元数据
     */
    void saveMetadata(Long postId, SavePostMetadataReqDTO req);

    /**
     * 发布帖子
     */
    void publishPost(Long postId);

    /**
     * 下线帖子
     */
    void unpublishPost(Long postId);

    /**
     * 获取帖子状态
     */
    PostRespDTO getPost(Long postId);

    /**
     * 删除帖子
     */
    void deletePost(Long postId);

    /**
     * 获取帖子版本历史
     */
    PostVersionRespDTO getVersions(Long postId);

    /**
     * 回滚帖子到指定版本
     */
    void rollbackPost(Long postId, RollbackVersionReqDTO req);
}
