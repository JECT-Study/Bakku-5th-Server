package com.ject.exchange.api.dto.response;

// Todo: api enum을 domain/application enum으로 통일하면 삭제
final class EnumMapper {

    private EnumMapper() {}

    static <E extends Enum<E>> E map(Enum<?> source, Class<E> target) {
        return source == null ? null : Enum.valueOf(target, source.name());
    }
}
