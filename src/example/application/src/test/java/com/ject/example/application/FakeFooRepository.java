package com.ject.example.application;

import com.ject.example.application.port.FooRepository;
import com.ject.example.domain.domain.Foo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 테스트용 가짜 저장소. 출력 포트가 인터페이스라서 DB 없이 서비스 로직을 검증할 수 있다.
 */
class FakeFooRepository implements FooRepository {

    private final Map<Long, Foo> store = new HashMap<>();
    private long sequence = 0;

    @Override
    public Foo save(Foo foo) {
        Long id = foo.getId() != null ? foo.getId() : ++sequence;
        Foo saved = Foo.restore(id, foo.getName(), foo.getStatus(), foo.getBars());
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Foo> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }
}
