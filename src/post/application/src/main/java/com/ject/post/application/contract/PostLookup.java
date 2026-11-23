package com.ject.post.application.contract;

import java.util.List;

public interface PostLookup {

    // 요청한 id 순서대로 반환. 중복은 한 번, 삭제 글은 DELETED로 포함, 없는 id는 빠짐
    List<PostBrief> findAllByIds(List<Long> postIds);
}
