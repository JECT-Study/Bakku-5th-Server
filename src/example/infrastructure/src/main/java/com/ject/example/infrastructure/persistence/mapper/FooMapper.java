package com.ject.example.infrastructure.persistence.mapper;

import com.ject.example.domain.domain.Bar;
import com.ject.example.domain.domain.Foo;
import com.ject.example.domain.domain.FooName;
import com.ject.example.infrastructure.persistence.entity.BarJpaEntity;
import com.ject.example.infrastructure.persistence.entity.FooJpaEntity;

/**
 * 도메인 객체 ↔ JPA 엔티티 변환. 도메인이 JPA를 모르도록 변환 책임을 infrastructure에 둔다.
 */
final class FooMapper {

    private FooMapper() {
    }

    static FooJpaEntity toEntity(Foo foo) {
        return new FooJpaEntity(
                foo.getId(),
                foo.getName().value(),
                foo.getStatus(),
                foo.getBars().stream()
                        .map(bar -> new BarJpaEntity(bar.getId(), bar.getContent()))
                        .toList()
        );
    }

    static Foo toDomain(FooJpaEntity entity) {
        return Foo.restore(
                entity.getId(),
                new FooName(entity.getName()),
                entity.getStatus(),
                entity.getBars().stream()
                        .map(bar -> Bar.restore(bar.getId(), bar.getContent()))
                        .toList()
        );
    }
}
