package com.ject.image.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.image.api.controller.api.ImageApi;
import com.ject.image.api.dto.request.ImageUploadRequest;
import com.ject.image.api.dto.response.ImageUploadResponse;
import com.ject.image.api.dto.response.UploadedImageResponse;
import com.ject.image.application.dto.ImageFileInfo;
import com.ject.image.application.service.ImageFileValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
public class ImageController implements ImageApi {

    private final ImageFileValidator imageFileValidator;
    private final CacheControl cacheControl;

    public ImageController(ImageFileValidator imageFileValidator,
                           @Value("${app.image.cache-max-age}") Duration cacheMaxAge) {
        this.imageFileValidator = imageFileValidator;
        this.cacheControl = CacheControl.maxAge(cacheMaxAge).cachePrivate();
    }

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
                .cacheControl(cacheControl)
                .body(ImageStubData.SAMPLE_PNG);
    }
}
