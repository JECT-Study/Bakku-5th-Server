package com.ject.post.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.dto.response.TagSuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Tag", description = "태그 자동완성")
@RequestMapping("/api/v1/tags")
public interface TagApi {

    @Operation(summary = "PO-9 태그 자동완성 (2차)", description = "입력 글자가 들어간 태그를 글 수 많은 순으로 보여줍니다. 글쓰기 · 수정 태그 칸에서만 씁니다.")
    @GetMapping
    ApiResponse<TagSuggestionResponse> suggestTags(
            @Parameter(description = "입력 중인 글자. 띄어쓰기와 앞의 #을 뺀 길이로 1~20자")
            @RequestParam("keyword") @NotBlank @Size(max = 20) String keyword,
            @Parameter(description = "1~20, 기본 10")
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(20) Integer size);
}
