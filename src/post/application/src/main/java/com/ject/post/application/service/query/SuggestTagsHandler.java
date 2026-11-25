package com.ject.post.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.post.application.dto.TagSuggestionResult;
import com.ject.post.application.dto.query.SuggestTagsQuery;
import com.ject.post.application.port.TagSuggestionReader;
import com.ject.post.domain.domain.TagKeyword;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuggestTagsHandler implements QueryHandler<SuggestTagsQuery, TagSuggestionResult> {

    private final TagSuggestionReader tagSuggestionReader;

    @Override
    @Transactional(readOnly = true)
    public TagSuggestionResult handle(SuggestTagsQuery query) {
        TagKeyword keyword = TagKeyword.of(query.keyword());
        return new TagSuggestionResult(tagSuggestionReader.suggest(keyword.value(), query.size()));
    }
}
