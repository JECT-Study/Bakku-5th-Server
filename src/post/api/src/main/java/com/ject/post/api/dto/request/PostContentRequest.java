package com.ject.post.api.dto.request;

import com.ject.post.api.dto.enums.PostCategory;

import java.util.List;

public interface PostContentRequest {

    List<String> imageKeys();

    String title();

    PostCategory category();

    Boolean opened();

    List<String> tags();

    String description();

    String wantedItem();

    PlaceRequest place();
}
