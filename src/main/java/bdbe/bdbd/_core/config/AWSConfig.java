package bdbe.bdbd._core.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "storage.s3.enabled", havingValue = "true")
@Slf4j
public class AWSConfig {

    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.proxy.host}")
    private String proxyHost;

    @Value("${cloud.aws.proxy.port}")
    private int proxyPort;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Bean
    public AmazonS3Client amazonS3ClientProd() {
        log.info("Initializing AmazonS3 client for 'prod' profile.");

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setConnectionTimeout(60000);  // 60,000 ms = 60 s
        clientConfiguration.setSocketTimeout(60000);

        if (!StringUtils.isEmpty(proxyHost) && proxyPort > 0) {
            log.info("Setting up proxy: Host = {}, Port = {}", proxyHost, proxyPort);
            clientConfiguration.setProxyHost(proxyHost);
            clientConfiguration.setProxyPort(proxyPort);
        } else {
            log.warn("Proxy settings are empty or incorrect. Proxy will not be used.");
        }

        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        log.info("AWS credentials configured for S3 client.");

        AmazonS3Client amazonS3Client = (AmazonS3Client) AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(clientConfiguration)
                .withRegion(region)
                .build();

        if (amazonS3Client != null) {
            log.info("Successfully initialized AmazonS3 client.");
        } else {
            log.error("Failed to initialize AmazonS3 client.");
        }

        return amazonS3Client;
    }
}
