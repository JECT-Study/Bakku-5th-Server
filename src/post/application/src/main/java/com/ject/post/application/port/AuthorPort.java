package com.ject.post.application.port;

import com.ject.post.application.dto.AuthorInfo;

import java.util.Collection;
import java.util.Map;

public interface AuthorPort {

    Map<Long, AuthorInfo> findAuthors(Collection<Long> authorIds);
}
