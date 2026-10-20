package com.ject.example.application.port;

import com.ject.example.domain.domain.Foo;

import java.util.Optional;

/**
 * 출력 포트: application이 필요로 하는 저장 기능을 정의하고, 구현은 infrastructure가 담당한다.
 */
public interface FooRepository {

    Foo save(Foo foo);

    Optional<Foo> findById(Long id);
}
