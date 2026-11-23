package com.ject.exchange.application.service.command;

import com.ject.exchange.application.port.ExchangeImageRejectedException;
import com.ject.exchange.application.port.ImagePort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 테스트용 가짜 사진 Port. image BC의 확정 규칙을 흉내 낸다.
 * tmp key {@code exchange/tmp/{회원}/{파일}}은 {@code exchange/{교환 ID}/{파일}}로 바꾸고, 이 교환의 정식 key는 그대로 둔다.
 * 거부할 key를 정해 두면 그 key의 순서(1부터)로 거부한다.
 */
class FakeImagePort implements ImagePort {

    private static final String TMP_PREFIX = "exchange/tmp/";

    private final List<Call> calls = new ArrayList<>();
    private final Set<String> rejectedKeys = new HashSet<>();

    @Override
    public List<String> attachToExchange(Long uploaderId, Long exchangeId, List<String> imageKeys) {
        calls.add(new Call(uploaderId, exchangeId, List.copyOf(imageKeys)));
        IntStream.range(0, imageKeys.size())
                .filter(index -> rejectedKeys.contains(imageKeys.get(index)))
                .findFirst()
                .ifPresent(index -> {
                    throw new ExchangeImageRejectedException(index + 1, imageKeys.get(index));
                });
        return imageKeys.stream()
                .map(key -> toFinalKey(exchangeId, key))
                .toList();
    }

    /** image BC가 이 key를 쓸 수 없다고 거부하게 한다. */
    void reject(String key) {
        rejectedKeys.add(key);
    }

    List<Call> calls() {
        return calls;
    }

    static String toFinalKey(Long exchangeId, String key) {
        if (!key.startsWith(TMP_PREFIX)) {
            return key;
        }
        String fileName = key.substring(key.lastIndexOf('/') + 1);
        return "exchange/" + exchangeId + "/" + fileName;
    }

    record Call(Long uploaderId, Long exchangeId, List<String> imageKeys) {
    }
}
