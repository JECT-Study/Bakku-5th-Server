package com.ject.post.domain.domain;

public record Place(
        double lat,
        double lng,
        String name,
        String regionName,
        String memo
) {

    private static final String SEJONG = "세종특별자치시";

    public String displayRegionName() {
        String[] words = regionName.strip().split("\\s+");
        if (words.length <= 2) {
            return String.join(" ", words).replace(SEJONG, "세종시");
        }
        return words[words.length - 2] + " " + words[words.length - 1];
    }
}
