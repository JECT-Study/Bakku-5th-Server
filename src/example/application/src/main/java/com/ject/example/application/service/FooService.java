package com.ject.example.application.service;

import com.ject.example.application.dto.FooResult;
import com.ject.example.application.port.FooRepository;
import com.ject.example.domain.domain.Foo;
import com.ject.example.domain.domain.FooName;
import com.ject.example.domain.exception.FooNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유스케이스 흐름(조회 → 도메인 메서드 호출 → 저장)만 조율하고, 규칙 판단은 도메인에 맡긴다.
 * 도메인 객체와 JPA 엔티티가 분리되어 있어 변경 감지가 없으므로, 변경 후에는 명시적으로 save한다.
 */
@Service
@Transactional
public class FooService {

    private final FooRepository fooRepository;

    public FooService(FooRepository fooRepository) {
        this.fooRepository = fooRepository;
    }

    public FooResult create(String name) {
        Foo foo = Foo.create(new FooName(name));
        return FooResult.from(fooRepository.save(foo));
    }

    @Transactional(readOnly = true)
    public FooResult get(Long id) {
        return FooResult.from(findFoo(id));
    }

    public FooResult rename(Long id, String name) {
        Foo foo = findFoo(id);
        foo.rename(new FooName(name));
        return FooResult.from(fooRepository.save(foo));
    }

    public FooResult addBar(Long id, String content) {
        Foo foo = findFoo(id);
        foo.addBar(content);
        return FooResult.from(fooRepository.save(foo));
    }

    public FooResult archive(Long id) {
        Foo foo = findFoo(id);
        foo.archive();
        return FooResult.from(fooRepository.save(foo));
    }

    private Foo findFoo(Long id) {
        return fooRepository.findById(id)
                .orElseThrow(() -> new FooNotFoundException(id));
    }
}
