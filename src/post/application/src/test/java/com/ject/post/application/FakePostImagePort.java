package com.ject.post.application;

import com.ject.post.application.port.PostImagePort;
import com.ject.post.domain.exception.InvalidImageKeyException;

import java.util.ArrayList;
import java.util.List;

public class FakePostImagePort implements PostImagePort {

    @Override
    public List<String> attachToPost(Long authorId, Long postId, List<String> imageKeys) {
        String tmpPrefix = "post/tmp/" + authorId + "/";
        String finalPrefix = "post/" + postId + "/";
        List<String> finalKeys = new ArrayList<>();
        for (int i = 0; i < imageKeys.size(); i++) {
            String key = imageKeys.get(i);
            if (key.startsWith(finalPrefix)) {
                finalKeys.add(key);
            } else if (key.startsWith(tmpPrefix)) {
                finalKeys.add(finalPrefix + key.substring(tmpPrefix.length()));
            } else {
                throw new InvalidImageKeyException(i + 1, key);
            }
        }
        return finalKeys;
    }
}
