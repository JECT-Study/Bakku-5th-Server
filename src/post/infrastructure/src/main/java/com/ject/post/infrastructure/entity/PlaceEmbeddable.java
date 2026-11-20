package com.ject.post.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceEmbeddable {

    @Column(name = "place_lat", nullable = false)
    private double lat;

    @Column(name = "place_lng", nullable = false)
    private double lng;

    @Column(name = "place_name", nullable = false, length = 100)
    private String name;

    @Column(name = "place_region_name", nullable = false, length = 100)
    private String regionName;

    @Column(name = "place_memo", length = 30)
    private String memo;

    @Builder
    public PlaceEmbeddable(double lat, double lng, String name, String regionName, String memo) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.regionName = regionName;
        this.memo = memo;
    }
}
