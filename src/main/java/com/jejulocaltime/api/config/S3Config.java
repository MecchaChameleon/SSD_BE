package com.jejulocaltime.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * local 프로필이 아닐 때 S3Client를 등록한다. 자격증명은 DefaultCredentialsProvider가
 * AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY 환경변수(또는 IAM Role 등)에서 읽으므로 코드에는 두지 않는다.
 */
@Configuration
@Profile("!local")
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${aws.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}
