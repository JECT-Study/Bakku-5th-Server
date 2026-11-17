package com.ject.post.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.FakePostRepository;
import com.ject.post.application.dto.command.CreatePostCommand;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.InvalidImageKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatePostHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T05:30:00Z");

    private FakePostRepository postRepository;
    private CreatePostHandler handler;

    @BeforeEach
    void setUp() {
        postRepository = new FakePostRepository(NOW);
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new CreatePostHandler(postRepository, serviceTime);
    }

    @Test
    @DisplayName("작성하면 글 ID를 돌려주고, 사진 key를 글 경로로 옮겨 저장한다")
    void handle_savesPostWithMovedImages() {
        // when
        Long postId = handler.handle(command(List.of("post/tmp/3/8f14e45f.jpg", "post/tmp/3/c9f0f895.png"), 60));

        // then
        Post saved = postRepository.findById(postId).orElseThrow();
        assertEquals(List.of("post/1/8f14e45f.jpg", "post/1/c9f0f895.png"), saved.getImageKeys());
        assertEquals(PostStatus.AVAILABLE, saved.getStatus());
        assertEquals(PostCategory.CHARACTER, saved.getCategory());
        assertEquals(NOW.plus(Duration.ofMinutes(60)), saved.getNowExchangeEndsAt());
        assertEquals(List.of("치이카와", "하치와레"), saved.getTags());
    }

    @Test
    @DisplayName("다른 회원이 올린 사진이 있으면 저장하지 않고 POST_005")
    void handle_otherMembersImage_throws() {
        // when
        assertThrows(InvalidImageKeyException.class,
                () -> handler.handle(command(List.of("post/tmp/7/8f14e45f.jpg"), null)));

        // then
        assertTrue(postRepository.findById(1L).isEmpty());
    }

    private static CreatePostCommand command(List<String> imageKeys, Integer nowExchangeMinutes) {
        return new CreatePostCommand(
                3L,
                imageKeys,
                "치이카와 하치와레 가챠 키링",
                "CHARACTER",
                false,
                List.of("치이카와", "하치 와레"),
                "치이카와 가챠 3탄에서 뽑았어요.",
                null,
                new CreatePostCommand.PlaceCommand(37.5563, 126.9236, "스타벅스 홍대동교점",
                        "서울특별시 마포구 동교동", "AK플라자 주변"),
                nowExchangeMinutes);
    }
}
