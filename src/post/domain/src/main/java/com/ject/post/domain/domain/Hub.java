package com.ject.post.domain.domain;

import java.util.List;

public enum Hub {

    HONGDAE(List.of(
            vertex(126.92072, 37.55695), vertex(126.92419, 37.55826), vertex(126.92766, 37.55757),
            vertex(126.92823, 37.55572), vertex(126.92669, 37.55434), vertex(126.92361, 37.55373),
            vertex(126.92111, 37.55449), vertex(126.91985, 37.55572))),
    YONGSAN(List.of(
            vertex(126.9628, 37.52976), vertex(126.96624, 37.52994), vertex(126.96715, 37.5286),
            vertex(126.96579, 37.52697), vertex(126.96303, 37.52625), vertex(126.96208, 37.52773))),
    SEONGSU(List.of(
            vertex(127.053, 37.54564), vertex(127.05651, 37.54536), vertex(127.05748, 37.54424),
            vertex(127.05651, 37.54325), vertex(127.05379, 37.54339), vertex(127.05256, 37.54452))),
    SINDORIM(List.of(
            vertex(126.88732, 37.50888), vertex(126.89115, 37.50999), vertex(126.89328, 37.50905),
            vertex(126.89396, 37.50735), vertex(126.89205, 37.50579), vertex(126.88902, 37.50613),
            vertex(126.88689, 37.50735))),
    GUKJEON(List.of(
            vertex(127.01524, 37.48612), vertex(127.01755, 37.48643), vertex(127.02024, 37.48597),
            vertex(127.02124, 37.48466), vertex(127.02082, 37.48325), vertex(127.01851, 37.48322),
            vertex(127.01678, 37.48382), vertex(127.01495, 37.48417), vertex(127.01462, 37.48535))),
    UNCLASSIFIED(List.of());

    private final List<Vertex> polygon;

    Hub(List<Vertex> polygon) {
        this.polygon = polygon;
    }

    public static Hub of(double lat, double lng) {
        for (Hub hub : values()) {
            if (hub.contains(lat, lng)) {
                return hub;
            }
        }
        return UNCLASSIFIED;
    }

    boolean contains(double lat, double lng) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Vertex a = polygon.get(i);
            Vertex b = polygon.get(j);
            boolean crosses = (a.lat() > lat) != (b.lat() > lat)
                    && lng < (b.lng() - a.lng()) * (lat - a.lat()) / (b.lat() - a.lat()) + a.lng();
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static Vertex vertex(double lng, double lat) {
        return new Vertex(lng, lat);
    }

    private record Vertex(double lng, double lat) {
    }
}
