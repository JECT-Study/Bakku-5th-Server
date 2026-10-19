package com.ject.example.infrastructure.persistence.mapper;

import com.ject.example.application.port.FooRepository;
import com.ject.example.domain.domain.Foo;
import com.ject.example.infrastructure.persistence.entity.FooJpaEntity;
import com.ject.example.infrastructure.persistence.repository.FooJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * application의 출력 포트(FooRepository)를 JPA로 구현한 어댑터.
 */
@Repository
public class FooRepositoryAdapter implements FooRepository {

    private final FooJpaRepository fooJpaRepository;

    public FooRepositoryAdapter(FooJpaRepository fooJpaRepository) {
        this.fooJpaRepository = fooJpaRepository;
    }

    @Override
    public Foo save(Foo foo) {
        FooJpaEntity saved = fooJpaRepository.save(FooMapper.toEntity(foo));
        return FooMapper.toDomain(saved);
    }

    @Override
    public Optional<Foo> findById(Long id) {
        return fooJpaRepository.findById(id)
                .map(FooMapper::toDomain);
    }
}
