package com.ject.chat.infrastructure.repository;

import com.ject.chat.infrastructure.ChatPostgresTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
@Import(PostgresChatRoomLockRepository.class)
class PostgresChatRoomLockRepositoryTest extends ChatPostgresTest {

    @Autowired
    private PostgresChatRoomLockRepository repository;

    @Test
    @DisplayName("게시글과 요청자 조합으로 PostgreSQL transaction advisory lock을 획득한다")
    void lock_validCombination_acquiresTransactionLock() {
        assertDoesNotThrow(() -> repository.lock(10L, 100L));
    }
}
