package bdbe.bdbd._core.utils;

import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.exception.InternalServerError;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 파일을 Amazon S3에 업로드하는 기능 제공
 */
@Service
@Slf4j
public class FileUploadUtil {

    private final AmazonS3 s3Client;
    private final String bucketName;
    private final boolean s3Enabled;
    private final Path localDirectory;
    private final String localBaseUrl;
    static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");

    public FileUploadUtil(
            @Value("${storage.s3.enabled:false}") boolean s3Enabled,
            @Value("${cloud.aws.credentials.accessKey}") String accessKey,
            @Value("${cloud.aws.credentials.secretKey}") String secretKey,
            @Value("${cloud.aws.region.static}") String region,
            @Value("${cloud.aws.proxy.host}") String proxyHost,
            @Value("${cloud.aws.proxy.port}") int proxyPort,
            @Value("${cloud.aws.s3.bucket}") String bucketName,
            @Value("${storage.local.directory:./uploads}") String localDirectory,
            @Value("${storage.local.base-url:/uploads}") String localBaseUrl) {

        this.s3Enabled = s3Enabled;
        this.bucketName = bucketName;
        this.localDirectory = Paths.get(localDirectory).toAbsolutePath().normalize();
        this.localBaseUrl = localBaseUrl.replaceAll("/+$", "");

        if (!s3Enabled) {
            this.s3Client = null;
            log.info("S3 file upload is disabled for the active profile.");
            return;
        }

        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(region);

        if ("prod".equals(System.getProperty("spring.profiles.active")) &&
                proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setProxyHost(proxyHost);
            clientConfiguration.setProxyPort(proxyPort);
            builder.withClientConfiguration(clientConfiguration);
        }

        this.s3Client = builder.build();
    }

    public void validateFiles(MultipartFile[] files) throws BadRequestError {
        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (originalFilename != null) {
                int i = originalFilename.lastIndexOf('.');
                if (i > 0) {
                    extension = originalFilename.substring(i).toLowerCase();
                }
            }

            if (!FileUploadUtil.ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BadRequestError(
                        BadRequestError.ErrorCode.VALIDATION_FAILED,
                        Collections.singletonMap("Invalid file extension for file: " + originalFilename, "Only JPG, JPEG, and PNG are allowed.")
                );
            }
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        validateFiles(new MultipartFile[]{file}); // This will throw BadRequestError if validation fails

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        if (!s3Enabled) {
            Files.createDirectories(localDirectory);
            Path destination = localDirectory.resolve(uniqueFilename).normalize();
            if (!destination.getParent().equals(localDirectory)) {
                throw new IOException("Invalid local upload path.");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return localBaseUrl + "/" + uniqueFilename;
        }

        String mimeType = file.getContentType();
        String keyName = "uploads/" + uniqueFilename;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(mimeType);
        metadata.setContentLength(file.getSize());
        metadata.setContentDisposition("inline");

        s3Client.putObject(bucketName, keyName, file.getInputStream(), metadata);

        return s3Client.getUrl(bucketName, keyName).toExternalForm();
    }

    public List<String> uploadFiles(List<MultipartFile> files) {
        List<String> uploadResults = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String result = uploadFile(file);
                uploadResults.add(result);
            } catch (Exception e) {
                log.error("File upload failed: " + e.getMessage(), e);
                throw new InternalServerError(
                        InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR,
                        Collections.singletonMap("File", "File upload failed: " + e.getMessage()));
            }
        }

        return uploadResults;
    }
}
