package com.ject.post.application.contract;

import java.util.List;

// 하나라도 바꿀 수 없으면 PostStatusChangeRejectedException, 아무것도 바뀌지 않음 (호출한 트랜잭션 롤백)
public interface PostStatusChanger {

    // 교환 가능 → 예약중
    void reserve(List<Long> postIds);

    // 예약중 → 교환 가능
    void release(List<Long> postIds);

    // 예약중 → 교환 완료
    void complete(List<Long> postIds);
}
