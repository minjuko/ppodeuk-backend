package bdbe.bdbd.service.file;

import bdbe.bdbd._core.exception.ForbiddenError;
import bdbe.bdbd._core.exception.InternalServerError;
import bdbe.bdbd._core.exception.NotFoundError;
import bdbe.bdbd._core.utils.FileUploadUtil;
import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.file.File;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.repository.file.FileJPARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileJPARepository fileJPARepository;

    private final FileUploadUtil fileUploadUtil;

    @Transactional
    public List<ReservationResponse.ImageDTO> uploadAndSaveFiles(MultipartFile[] images, Carwash carwash) {
        List<File> existingFiles = fileJPARepository.findByCarwash_IdAndIsDeletedFalse(carwash.getId());
        for (File file : existingFiles) {
            file.changeDeletedFlag(true);
        }
        fileJPARepository.saveAll(existingFiles);

        List<ReservationResponse.ImageDTO> updatedImages = new ArrayList<>();
        try {
            List<String> imageUrls = fileUploadUtil.uploadFiles(Arrays.asList(images));

            List<File> savedFiles = saveFileEntities(imageUrls, carwash);

            for (File file : savedFiles) {
                updatedImages.add(new ReservationResponse.ImageDTO(file));
            }

        } catch (Exception e) {
            log.error("File upload and save failed: " + e.getMessage(), e);
            throw new InternalServerError(
                    InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR,
                    Collections.singletonMap("File", "File upload and saved failed: " + e.getMessage()));
        }

        return updatedImages;
    }

    private List<File> saveFileEntities(List<String> imageUrls, Carwash carwash) {
        List<File> files = new ArrayList<>();
        try {
            for (String imageUrl : imageUrls) {
                File newFile = File.builder()
                        .name(imageUrl.substring(imageUrl.lastIndexOf("/") + 1))
                        .url(imageUrl)
                        .uploadedAt(LocalDateTime.now())
                        .carwash(carwash)
                        .build();
                files.add(newFile);
            }
            files = fileJPARepository.saveAll(files);
            log.info("File entities saved successfully");
        } catch (Exception e) {
            log.error("Saving file entities failed: " + e.getMessage(), e);
            throw new InternalServerError(
                    InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR,
                    Collections.singletonMap("File", "File saved failed: " + e.getMessage()));
        }

        return files;
    }

    public void deleteFile(Long fileId, Member member) {

        File file = fileJPARepository.findById(fileId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("fileId", "File id " + fileId + " not found.")));
        if (!Objects.equals(file.getCarwash().getMember().getId(), member.getId())) {
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("MemberId", "Member is not the owner of the Carwash related to file.")
            );

        }
        file.changeDeletedFlag(true);
        fileJPARepository.save(file);
    }

}
