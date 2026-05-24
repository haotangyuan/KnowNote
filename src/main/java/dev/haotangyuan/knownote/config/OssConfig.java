package dev.haotangyuan.knownote.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class OssConfig {
    private final OssProperties ossProperties;

    @Bean
    @ConditionalOnExpression("not '${oss.endpoint:}'.blank")
    public S3Presigner createOssPresigner() {
        return S3Presigner.builder()
            .endpointOverride(URI.create(ossProperties.getEndpoint()))
            .region(Region.of("us-east-1"))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ossProperties.getAccessKeyId(), ossProperties.getSecretAccessKey())))
            .build();
    }

    @Bean
    @ConditionalOnExpression("not '${oss.endpoint:}'.blank")
    public S3Client createS3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(ossProperties.getEndpoint()))
            .region(Region.of("us-east-1"))
            .forcePathStyle(true)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ossProperties.getAccessKeyId(), ossProperties.getSecretAccessKey())))
            .build();
    }
}
