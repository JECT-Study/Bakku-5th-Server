package com.ject.post.application.service.contract;

import com.ject.post.application.contract.PostBrief;
import com.ject.post.application.contract.PostLookup;
import com.ject.post.application.port.PostSummaryReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostLookupService implements PostLookup {

    private final PostSummaryReader postSummaryReader;

    @Override
    @Transactional(readOnly = true)
    public List<PostBrief> findAllByIds(List<Long> postIds) {
        List<Long> ids = List.copyOf(new LinkedHashSet<>(postIds));
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, PostSummaryReader.Brief> found = postSummaryReader.findBriefs(ids).stream()
                .collect(Collectors.toMap(PostSummaryReader.Brief::id, Function.identity()));
        return ids.stream()
                .map(found::get)
                .filter(Objects::nonNull)
                .map(brief -> new PostBrief(brief.id(), brief.authorId(), brief.status().name(), brief.title(),
                        brief.thumbnailKey()))
                .toList();
    }
}
