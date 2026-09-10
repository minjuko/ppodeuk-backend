package bdbe.bdbd.file;

import bdbe.bdbd._core.utils.FileUploadUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileUploadUtilTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesImageLocallyWhenS3IsDisabled() throws Exception {
        FileUploadUtil uploadUtil = new FileUploadUtil(
                false,
                "",
                "",
                "ap-northeast-2",
                "",
                0,
                "",
                tempDirectory.toString(),
                "http://localhost:8080/uploads/");
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "carwash.jpg",
                "image/jpeg",
                "local-image".getBytes());

        String imageUrl = uploadUtil.uploadFile(image);
        String storedFilename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

        assertThat(imageUrl).startsWith("http://localhost:8080/uploads/");
        assertThat(Files.readString(tempDirectory.resolve(storedFilename)))
                .isEqualTo("local-image");
    }
}
