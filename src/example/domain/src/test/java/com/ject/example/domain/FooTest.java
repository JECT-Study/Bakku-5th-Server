package com.ject.example.domain;

import com.ject.example.domain.domain.Bar;
import com.ject.example.domain.domain.Foo;
import com.ject.example.domain.domain.FooName;
import com.ject.example.domain.domain.FooStatus;
import com.ject.example.domain.exception.BarLimitExceededException;
import com.ject.example.domain.exception.FooArchivedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FooTest {

    @Test
    @DisplayName("새 Foo는 ACTIVE 상태이고 Bar가 없다")
    void create() {
        Foo foo = Foo.create(new FooName("foo"));

        assertEquals(FooStatus.ACTIVE, foo.getStatus());
        assertTrue(foo.getBars().isEmpty());
    }

    @Test
    @DisplayName("Bar는 최대 5개까지만 추가할 수 있다")
    void addBarLimit() {
        Foo foo = Foo.create(new FooName("foo"));
        for (int i = 1; i <= 5; i++) {
            foo.addBar("bar" + i);
        }

        assertThrows(BarLimitExceededException.class, () -> foo.addBar("bar6"));
        assertEquals(5, foo.getBars().size());
    }

    @Test
    @DisplayName("보관된 Foo는 이름 변경, Bar 추가, 재보관을 할 수 없다")
    void archivedFooCannotChange() {
        Foo foo = Foo.create(new FooName("foo"));
        foo.archive();

        assertThrows(FooArchivedException.class, () -> foo.rename(new FooName("new")));
        assertThrows(FooArchivedException.class, () -> foo.addBar("bar"));
        assertThrows(FooArchivedException.class, foo::archive);
    }

    @Test
    @DisplayName("getBars로 받은 목록은 외부에서 수정할 수 없다")
    void barsAreUnmodifiable() {
        Foo foo = Foo.create(new FooName("foo"));

        assertThrows(UnsupportedOperationException.class,
                () -> foo.getBars().add(Bar.restore(1L, "bar")));
    }
}
