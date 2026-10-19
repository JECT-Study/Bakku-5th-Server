package com.ject.example.application.dto;

import com.ject.example.domain.domain.Foo;

import java.util.List;

/**
 * api 계층은 domain을 모르므로, 도메인 객체 대신 이 결과 객체를 반환한다.
 */
public record FooResult(Long id, String name, String status, List<BarResult> bars) {

    public record BarResult(Long id, String content) {
    }

    public static FooResult from(Foo foo) {
        List<BarResult> bars = foo.getBars().stream()
                .map(bar -> new BarResult(bar.getId(), bar.getContent()))
                .toList();
        return new FooResult(foo.getId(), foo.getName().value(), foo.getStatus().name(), bars);
    }
}
