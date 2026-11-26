package com.ject.post.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.FakeMemberRestrictionPort;
import com.ject.post.application.FakePostImagePort;
import com.ject.post.application.FakePostRepository;
import com.ject.post.application.dto.command.CreatePostCommand;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.InvalidImageKeyException;
import com.ject.post.domain.exception.RestrictedMemberException;
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

class CreatePostHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T05:30:00Z");

    private FakePostRepository postRepository;
    private FakeMemberRestrictionPort memberRestrictionPort;
    private CreatePostHandler handler;

    @BeforeEach
    void setUp() {
        postRepository = new FakePostRepository(NOW);
        memberRestrictionPort = new FakeMemberRestrictionPort();
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new CreatePostHandler(postRepository, new FakePostImagePort(), memberRestrictionPort, serviceTime);
    }

    @Test
    @DisplayName("이용 제한 회원이면 POST_006이고 글을 저장하지 않는다")
    void handle_restrictedMember_throws() {
        // given
        memberRestrictionPort.restrict(3L);

        // when · then
        assertThrows(RestrictedMemberException.class,
                () -> handler.handle(command(List.of("post/tmp/3/8f14e45f.jpg"), null)));
        assertEquals(0, postRepository.saveCalls);
    }

    @Test
    @DisplayName("작성하면 글 ID를 돌려주고, 이미지 모듈이 돌려준 정식 key로 저장한다")
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
    @DisplayName("이미지 모듈이 사진을 거절하면 POST_005를 그대로 던진다")
    void handle_rejectedImage_throws() {
        // when
        InvalidImageKeyException e = assertThrows(InvalidImageKeyException.class,
                () -> handler.handle(command(List.of("post/tmp/3/8f14e45f.jpg", "post/tmp/7/c9f0f895.png"), null)));

        // then
        assertEquals("2번째 이미지를 사용할 수 없습니다. key=post/tmp/7/c9f0f895.png", e.getMessage());
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
