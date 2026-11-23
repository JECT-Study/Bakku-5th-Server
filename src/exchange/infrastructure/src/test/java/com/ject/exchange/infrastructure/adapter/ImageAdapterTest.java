package com.ject.exchange.infrastructure.adapter;

import com.ject.exchange.application.port.ExchangeImageRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 교환 사진 확정을 image 계약(ImageAttachment)으로 하고 거부를 교환 Port 예외로 바꾸는지 확인한다 (#108).
 */
class ImageAdapterTest {

    private static final Long MEMBER_A = 101L;
    private static final Long EXCHANGE_ID = 1L;
    private static final String K1 = "exchange/tmp/101/0b1c2d3e-0001.jpg";
    private static final String K2 = "exchange/tmp/101/0b1c2d3e-0002.jpg";
    private static final String K2_FINAL = "exchange/1/0b1c2d3e-0002.jpg";
    private static final String K3 = "exchange/tmp/101/0b1c2d3e-0003.jpg";
    private static final String OTHER_MEMBER_TMP_KEY = "exchange/tmp/103/0b1c2d3e-0009.jpg";

    private FakeImageAttachment imageAttachment;
    private ImageAdapter imageAdapter;

    @BeforeEach
    void setUp() {
        imageAttachment = new FakeImageAttachment();
        imageAdapter = new ImageAdapter(imageAttachment);
    }

    @Test
    @DisplayName("attachToExchange는 용도 EXCHANGE · 업로더 · 교환 id · key 순서 그대로 확정을 요청한다")
    void attachToExchange_finalizesWithExchangePurpose() {
        imageAdapter.attachToExchange(MEMBER_A, EXCHANGE_ID, List.of(K1, K2));

        assertEquals(List.of(new FakeImageAttachment.FinalizeCall("EXCHANGE", MEMBER_A, EXCHANGE_ID, List.of(K1, K2))),
                imageAttachment.finalizeCalls());
    }

    @Test
    @DisplayName("attachToExchange는 image BC가 돌려준 정식 key를 같은 순서로 돌려준다")
    void attachToExchange_returnsFinalizedKeysInOrder() {
        List<String> result = imageAdapter.attachToExchange(MEMBER_A, EXCHANGE_ID, List.of(K2_FINAL, K3));

        assertEquals(List.of(K2_FINAL, "exchange/1/0b1c2d3e-0003.jpg"), result);
    }

    @Test
    @DisplayName("image BC가 두 번째 key를 거부하면 순서 2와 그 key를 담은 ExchangeImageRejectedException이다")
    void attachToExchange_rejected_throwsExchangeImageRejected() {
        imageAttachment.reject(OTHER_MEMBER_TMP_KEY);

        ExchangeImageRejectedException exception = assertThrows(ExchangeImageRejectedException.class,
                () -> imageAdapter.attachToExchange(MEMBER_A, EXCHANGE_ID, List.of(K1, OTHER_MEMBER_TMP_KEY)));

        assertEquals(2, exception.getOrder());
        assertEquals(OTHER_MEMBER_TMP_KEY, exception.getKey());
    }
}
