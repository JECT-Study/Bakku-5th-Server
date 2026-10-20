package com.ject.example.application;

import com.ject.example.application.dto.FooResult;
import com.ject.example.application.service.FooService;
import com.ject.example.domain.exception.FooArchivedException;
import com.ject.example.domain.exception.FooNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FooServiceTest {

    private FooService fooService;

    @BeforeEach
    void setUp() {
        fooService = new FooService(new FakeFooRepository());
    }

    @Test
    @DisplayName("Foo를 생성하면 id가 부여되고 조회할 수 있다")
    void createAndGet() {
        FooResult created = fooService.create("foo");

        assertNotNull(created.id());
        assertEquals("foo", fooService.get(created.id()).name());
    }

    @Test
    @DisplayName("없는 Foo를 조회하면 FooNotFoundException이 발생한다")
    void getNotFound() {
        assertThrows(FooNotFoundException.class, () -> fooService.get(999L));
    }

    @Test
    @DisplayName("이름 변경과 Bar 추가가 저장된다")
    void renameAndAddBar() {
        Long id = fooService.create("foo").id();

        fooService.rename(id, "renamed");
        fooService.addBar(id, "bar");

        FooResult result = fooService.get(id);
        assertEquals("renamed", result.name());
        assertEquals(1, result.bars().size());
    }

    @Test
    @DisplayName("보관된 Foo의 이름을 바꾸면 도메인 규칙에 의해 실패한다")
    void renameArchivedFoo() {
        Long id = fooService.create("foo").id();
        fooService.archive(id);

        assertThrows(FooArchivedException.class, () -> fooService.rename(id, "renamed"));
    }
}
