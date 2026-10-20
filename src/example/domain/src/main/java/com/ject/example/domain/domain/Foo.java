package com.ject.example.domain.domain;

import com.ject.example.domain.exception.BarLimitExceededException;
import com.ject.example.domain.exception.FooArchivedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root: Foo와 그 안의 Bar는 항상 Foo를 통해서만 변경되며,
 * 모든 도메인 규칙은 이 클래스 안에서 지켜진다.
 */
public class Foo {

    private static final int MAX_BAR_COUNT = 5;

    private final Long id;
    private FooName name;
    private FooStatus status;
    private final List<Bar> bars;

    private Foo(Long id, FooName name, FooStatus status, List<Bar> bars) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.bars = new ArrayList<>(bars);
    }

    public static Foo create(FooName name) {
        return new Foo(null, name, FooStatus.ACTIVE, List.of());
    }

    // 저장소에서 읽어온 데이터로 복원할 때 사용
    public static Foo restore(Long id, FooName name, FooStatus status, List<Bar> bars) {
        return new Foo(id, name, status, bars);
    }

    public void rename(FooName newName) {
        ensureActive();
        this.name = newName;
    }

    public void addBar(String content) {
        ensureActive();
        if (bars.size() >= MAX_BAR_COUNT) {
            throw new BarLimitExceededException(MAX_BAR_COUNT);
        }
        bars.add(Bar.create(content));
    }

    public void archive() {
        ensureActive();
        this.status = FooStatus.ARCHIVED;
    }

    private void ensureActive() {
        if (status == FooStatus.ARCHIVED) {
            throw new FooArchivedException();
        }
    }

    public Long getId() {
        return id;
    }

    public FooName getName() {
        return name;
    }

    public FooStatus getStatus() {
        return status;
    }

    public List<Bar> getBars() {
        return Collections.unmodifiableList(bars);
    }
}
