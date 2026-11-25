package com.ject.post.api.controller;

import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.controller.api.TagApi;
import com.ject.post.api.dto.response.TagSuggestionResponse;
import com.ject.post.application.dto.TagSuggestionResult;
import com.ject.post.application.dto.query.SuggestTagsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TagController implements TagApi {

    private final QueryBus queryBus;

    @Override
    public ApiResponse<TagSuggestionResponse> suggestTags(String keyword, Integer size) {
        TagSuggestionResult result = queryBus.send(new SuggestTagsQuery(keyword, size));
        return ApiResponse.success(new TagSuggestionResponse(result.tags()));
    }
}
