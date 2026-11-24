package com.ject.post.application.dto;

import java.util.List;

public record HubResult(String code, Coordinate center, List<Coordinate> polygon) {

    public record Coordinate(double lat, double lng) {
    }
}
