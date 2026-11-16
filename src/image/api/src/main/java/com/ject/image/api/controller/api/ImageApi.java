package com.ject.image.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.image.api.dto.request.ImageUploadRequest;
import com.ject.image.api.dto.response.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Image", description = "이미지 업로드 · 조회")
@RequestMapping("/api/v1/images")
public interface ImageApi {

    @Operation(summary = "IMG-1 이미지 업로드",
            description = "사진을 서버로 보내 임시 경로에 저장하고 key를 돌려줍니다. key를 저장 API(PO-1 등)에 넘기면 정식 경로로 옮겨집니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ImageUploadResponse> uploadImages(@Valid @ModelAttribute ImageUploadRequest request);

    @Operation(summary = "IMG-2 이미지 조회",
            description = "key의 이미지 파일을 내려줍니다. 응답의 images[].url을 Authorization 헤더와 함께 그대로 호출합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/{*key}")
    ResponseEntity<byte[]> getImage(
            @Parameter(description = "이미지 key. / 포함", example = "post/10/8f14e45f.jpg")
            @PathVariable("key") String key);
}
