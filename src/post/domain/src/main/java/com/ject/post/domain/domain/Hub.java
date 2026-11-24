package com.ject.post.domain.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum Hub {

    HONGDAE(point(126.92338, 37.55622), List.of(
            point(126.92072, 37.55695), point(126.92419, 37.55826), point(126.92766, 37.55757),
            point(126.92823, 37.55572), point(126.92669, 37.55434), point(126.92361, 37.55373),
            point(126.92111, 37.55449), point(126.91985, 37.55572))),
    YONGSAN(point(126.96484, 37.52864), List.of(
            point(126.9628, 37.52976), point(126.96624, 37.52994), point(126.96715, 37.5286),
            point(126.96579, 37.52697), point(126.96303, 37.52625), point(126.96208, 37.52773))),
    SEONGSU(point(127.05481, 37.54482), List.of(
            point(127.053, 37.54564), point(127.05651, 37.54536), point(127.05748, 37.54424),
            point(127.05651, 37.54325), point(127.05379, 37.54339), point(127.05256, 37.54452))),
    SINDORIM(point(126.89136, 37.50892), List.of(
            point(126.88732, 37.50888), point(126.89115, 37.50999), point(126.89328, 37.50905),
            point(126.89396, 37.50735), point(126.89205, 37.50579), point(126.88902, 37.50613),
            point(126.88689, 37.50735))),
    GUKJEON(point(127.01836, 37.48463), List.of(
            point(127.01524, 37.48612), point(127.01755, 37.48643), point(127.02024, 37.48597),
            point(127.02124, 37.48466), point(127.02082, 37.48325), point(127.01851, 37.48322),
            point(127.01678, 37.48382), point(127.01495, 37.48417), point(127.01462, 37.48535))),
    UNCLASSIFIED(null, List.of());

    private final Coordinate center;
    private final List<Coordinate> polygon;

    Hub(Coordinate center, List<Coordinate> polygon) {
        this.center = center;
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

    public static List<Hub> selectable() {
        return Arrays.stream(values())
                .filter(hub -> hub != UNCLASSIFIED)
                .toList();
    }

    boolean contains(double lat, double lng) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Coordinate a = polygon.get(i);
            Coordinate b = polygon.get(j);
            boolean crosses = (a.lat() > lat) != (b.lat() > lat)
                    && lng < (b.lng() - a.lng()) * (lat - a.lat()) / (b.lat() - a.lat()) + a.lng();
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static Coordinate point(double lng, double lat) {
        return new Coordinate(lat, lng);
    }

    public record Coordinate(double lat, double lng) {
    }
}
