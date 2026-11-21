package com.ject.exchange.domain.domain.appointment.vo;

/**
 * 약속 장소. 합의 시점 값으로 고정한다 (입력 형식은 API에서 검증)
 */
public record Place(String name, String address, Double lat, Double lng) {
}
