package com.ject.post.domain.domain;

public record Place(
        double lat,
        double lng,
        String name,
        String regionName,
        String memo
) {

    public Hub hub() {
        return Hub.of(lat, lng);
    }
}
