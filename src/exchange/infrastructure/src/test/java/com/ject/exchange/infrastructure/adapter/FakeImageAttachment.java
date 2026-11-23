package com.ject.exchange.infrastructure.adapter;

import com.ject.image.application.contract.ImageAttachment;
import com.ject.image.application.exception.ImageKeyRejectedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 테스트용 가짜 image 첨부 계약. tmp key는 {@code {용도 소문자}/{대상 ID}/{파일}}로 바꾸고, 그 밖의 key는 그대로 둔다.
 * 거부할 key를 정해 두면 image BC처럼 순서(1부터)와 key를 담아 거부한다.
 */
class FakeImageAttachment implements ImageAttachment {

    private final List<FinalizeCall> finalizeCalls = new ArrayList<>();
    private final Set<String> rejectedKeys = new HashSet<>();

    @Override
    public List<String> finalizeImages(String purpose, Long uploaderId, Long targetId, List<String> keys) {
        finalizeCalls.add(new FinalizeCall(purpose, uploaderId, targetId, List.copyOf(keys)));
        IntStream.range(0, keys.size())
                .filter(index -> rejectedKeys.contains(keys.get(index)))
                .findFirst()
                .ifPresent(index -> {
                    throw new ImageKeyRejectedException(index + 1, keys.get(index));
                });
        return keys.stream()
                .map(key -> key.contains("/tmp/")
                        ? purpose.toLowerCase() + "/" + targetId + "/" + key.substring(key.lastIndexOf('/') + 1)
                        : key)
                .toList();
    }

    @Override
    public void discardImages(String purpose, Long targetId, List<String> keys) {
        throw new IllegalStateException("이번 기능은 사진을 지우지 않는다");
    }

    void reject(String key) {
        rejectedKeys.add(key);
    }

    List<FinalizeCall> finalizeCalls() {
        return finalizeCalls;
    }

    record FinalizeCall(String purpose, Long uploaderId, Long targetId, List<String> keys) {
    }
}
