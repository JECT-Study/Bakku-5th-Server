package com.ject.post.application.port;

import java.util.List;

public interface TagSuggestionReader {

    List<String> suggest(String keyword, int limit);
}
