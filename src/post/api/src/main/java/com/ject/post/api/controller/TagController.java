package com.ject.post.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.controller.api.TagApi;
import com.ject.post.api.dto.response.TagSuggestionResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TagController implements TagApi {

    @Override
    public ApiResponse<TagSuggestionResponse> suggestTags(String keyword, Integer size) {
        return ApiResponse.success(new TagSuggestionResponse(List.of("치이카와", "치이카와가챠")));
    }
}
