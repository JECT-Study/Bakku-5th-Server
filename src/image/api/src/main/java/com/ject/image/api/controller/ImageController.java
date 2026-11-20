package com.ject.image.api.controller;

import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.response.ApiResponse;
import com.ject.image.api.controller.api.ImageApi;
import com.ject.image.api.dto.request.ImageUploadRequest;
import com.ject.image.api.dto.response.ImageUploadResponse;
import com.ject.image.api.dto.response.UploadedImageResponse;
import com.ject.image.application.dto.ImageUpload;
import com.ject.image.application.dto.StoredImage;
import com.ject.image.application.dto.command.UploadImagesCommand;
import com.ject.image.application.dto.query.GetImageQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
public class ImageController implements ImageApi {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final CacheControl cacheControl;

    public ImageController(CommandBus commandBus,
                           QueryBus queryBus,
                           @Value("${app.image.cache-max-age}") Duration cacheMaxAge) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
        this.cacheControl = CacheControl.maxAge(cacheMaxAge).cachePrivate();
    }

    @Override
    public ApiResponse<ImageUploadResponse> uploadImages(Long memberId, ImageUploadRequest request) {
        List<ImageUpload> files = request.files().stream()
                .map(file -> new ImageUpload(file.getSize(), file::getInputStream))
                .toList();
        List<String> keys = commandBus.send(
                new UploadImagesCommand(memberId, request.purpose().name(), files));

        List<UploadedImageResponse> uploads = keys.stream()
                .map(UploadedImageResponse::new)
                .toList();
        return ApiResponse.created(new ImageUploadResponse(uploads));
    }

    @Override
    public ResponseEntity<InputStreamResource> getImage(Long memberId, String key) {
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        StoredImage image = queryBus.send(new GetImageQuery(normalized, memberId));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.size())
                .cacheControl(cacheControl)
                .body(new InputStreamResource(image.content()));
    }
}
