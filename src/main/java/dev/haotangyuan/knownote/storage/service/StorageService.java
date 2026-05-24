package dev.haotangyuan.knownote.storage.service;

import cn.hutool.core.util.StrUtil;
import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;
import dev.haotangyuan.knownote.common.UserContext;
import dev.haotangyuan.knownote.config.OssProperties;
import dev.haotangyuan.knownote.post.domain.entity.PostDO;
import dev.haotangyuan.knownote.post.domain.mapper.PostMapper;
import dev.haotangyuan.knownote.storage.api.dto.req.UploadUrlReqDTO;
import dev.haotangyuan.knownote.storage.api.dto.resp.UploadUrlRespDTO;
import dev.haotangyuan.knownote.storage.domain.enums.UploadScene;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 存储服务
 */
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("not '${oss.endpoint:}'.blank")
public class StorageService {

    private final OssProperties ossProperties;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final PostMapper postMapper;

    /**
     * 根据场景获取 bucket
     */
    public String getBucket(UploadScene scene) {
        return scene.isPublic() ? ossProperties.getPublicBucket() : ossProperties.getPrivateBucket();
    }

    public UploadUrlRespDTO getUploadUrl(UploadUrlReqDTO req) {
        UploadScene uploadScene = UploadScene.fromScene(req.getScene());

        if (uploadScene != UploadScene.USER_AVATAR) {
            Long postId = Long.parseLong(req.getResourceId());
            PostDO post = postMapper.selectById(postId);
            if (post == null || !post.getCreatorId().equals(UserContext.getUserId())) {
                throw new BizException(ErrorCode.CLIENT_ERROR, "无权操作此资源");
            }
        }

        String bucket = getBucket(uploadScene);
        String path = uploadScene.getPath(req.getResourceId(), req.getExt());

        try {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(por -> por.bucket(bucket).key(path))
                .build();
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String uploadUrl = presignedRequest.url().toString();

            // 公开资源返回公开 URL，私有资源返回 key（前端需通过 getSignedUrl 获取访问 URL）
            String accessUrl = uploadScene.isPublic()
                ? buildPublicUrl(path)
                : path;

            return UploadUrlRespDTO.builder()
                .uploadUrl(uploadUrl)
                .accessUrl(accessUrl)
                .build();
        } catch (Exception e) {
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "生成预签名 URL 失败");
        }
    }

    /**
     * 列出指定前缀下的所有对象（私有 bucket）
     */
    public List<S3Object> listObjects(String prefix) {
        return listObjects(ossProperties.getPrivateBucket(), prefix);
    }

    /**
     * 列出指定 bucket 和前缀下的所有对象
     */
    public List<S3Object> listObjects(String bucket, String prefix) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.contents();
        } catch (Exception e) {
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "查询对象列表失败");
        }
    }

    /**
     * 构建公开访问 URL（使用 publicUrl）
     */
    public String buildPublicUrl(String key) {
        return StrUtil.format("{}/{}", ossProperties.getPublicUrl(), key);
    }

    /**
     * 复制对象（私有 bucket 内）
     */
    public void copyObject(String sourceKey, String destKey) {
        copyObject(ossProperties.getPrivateBucket(), sourceKey, ossProperties.getPrivateBucket(), destKey);
    }

    /**
     * 跨 bucket 复制对象（私有 → 公开）
     */
    public void copyToPublic(String sourceKey, String destKey) {
        copyObject(ossProperties.getPrivateBucket(), sourceKey, ossProperties.getPublicBucket(), destKey);
    }

    /**
     * 公开 bucket 内复制对象
     */
    public void copyPublicObject(String sourceKey, String destKey) {
        copyObject(ossProperties.getPublicBucket(), sourceKey, ossProperties.getPublicBucket(), destKey);
    }

    /**
     * 复制对象
     */
    public void copyObject(String sourceBucket, String sourceKey, String destBucket, String destKey) {
        try {
            CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(sourceKey)
                .destinationBucket(destBucket)
                .destinationKey(destKey)
                .build();
            s3Client.copyObject(request);
        } catch (Exception e) {
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "复制对象失败");
        }
    }

    /**
     * 删除公开 bucket 对象
     */
    public void deletePublicObject(String key) {
        deleteObject(ossProperties.getPublicBucket(), key);
    }

    /**
     * 删除对象
     */
    private void deleteObject(String bucket, String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "删除对象失败");
        }
    }

    /**
     * 批量删除对象（私有 bucket）
     */
    public void deleteObjects(List<String> keys) {
        deleteObjects(ossProperties.getPrivateBucket(), keys);
    }

    /**
     * 批量删除对象
     */
    public void deleteObjects(String bucket, List<String> keys) {
        if (keys.isEmpty()) return;
        try {
            List<ObjectIdentifier> objects = keys.stream()
                .map(k -> ObjectIdentifier.builder().key(k).build())
                .toList();
            Delete delete = Delete.builder().objects(objects).build();
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(delete)
                .build();
            s3Client.deleteObjects(request);
        } catch (Exception e) {
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "批量删除对象失败");
        }
    }

    /**
     * 获取签名读取 URL（私有 bucket）
     */
    public String getSignedUrl(String key, Duration duration) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(ossProperties.getPrivateBucket())
                .key(key)
                .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getRequest)
                .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "生成签名 URL 失败");
        }
    }

    /**
     * 获取 bucket 文件内容
     */
    public String getContent(String bucket, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asUtf8String();
        } catch (Exception e) {
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, "获取文件内容失败");
        }
    }


}
