package com.ject.image.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.image.api.controller.api.ImageApi;
import com.ject.image.api.dto.request.ImageUploadRequest;
import com.ject.image.api.dto.response.ImageUploadResponse;
import com.ject.image.api.dto.response.UploadedImageResponse;
import com.ject.image.application.dto.ImageFileInfo;
import com.ject.image.application.service.ImageFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ImageController implements ImageApi {

    private static final String CACHE_CONTROL = "private, max-age=86400";

    private final ImageFileValidator imageFileValidator;

    @Override
    public ApiResponse<ImageUploadResponse> uploadImages(ImageUploadRequest request) {
        List<MultipartFile> files = request.files();
        imageFileValidator.validate(files.stream()
                .map(file -> new ImageFileInfo(file.getContentType(), file.getSize()))
                .toList());

        List<String> contentTypes = files.stream().map(MultipartFile::getContentType).toList();
        List<UploadedImageResponse> uploads = ImageStubData.keys(request.purpose(), contentTypes).stream()
                .map(UploadedImageResponse::new)
                .toList();
        return ApiResponse.created(new ImageUploadResponse(uploads));
    }

    @Override
    public ResponseEntity<byte[]> getImage(String key) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                .body(ImageStubData.SAMPLE_PNG);
    }
}
