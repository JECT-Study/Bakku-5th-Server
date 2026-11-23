package com.ject.exchange.infrastructure.adapter;

import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostStatusRejectedException;
import com.ject.post.application.contract.PostStatusChangeRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 글 조회 · 글 상태 변경을 post 계약(PostLookup · PostStatusChanger)으로 하는지 확인한다 (#107).
 */
class PostAdapterTest {

    private static final Long POST_P = 10L;
    private static final Long POST_Q1 = 20L;
    private static final Long MISSING_POST = 999L;
    private static final Long LEGACY_STUB_POST = 1L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;

    private FakePostLookup postLookup;
    private FakePostStatusChanger postStatusChanger;
    private PostAdapter postAdapter;

    @BeforeEach
    void setUp() {
        postLookup = new FakePostLookup();
        postStatusChanger = new FakePostStatusChanger();
        postAdapter = new PostAdapter(postLookup, postStatusChanger);

        postLookup.register(POST_P, MEMBER_B, "AVAILABLE", "P", "post/10/1.jpg");
        postLookup.register(POST_Q1, MEMBER_A, "AVAILABLE", "Q1", "post/20/1.jpg");
    }

    // ---- AC-1 글 조회 ----

    @Test
    @DisplayName("findById는 post 글 요약을 id · 제목 · 사진 key · 작성자 · 상태로 바꾼다")
    void findById_mapsPostBrief() {
        Optional<PostInfo> found = postAdapter.findById(POST_P);

        assertEquals(Optional.of(new PostInfo(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.AVAILABLE)),
                found);
        assertEquals(List.of(List.of(POST_P)), postLookup.calls());
    }

    @Test
    @DisplayName("findById는 post BC에 없는 글이면 비어 있다")
    void findById_missing_returnsEmpty() {
        assertTrue(postAdapter.findById(MISSING_POST).isEmpty());
    }

    @Test
    @DisplayName("예전 stub 글 1번도 post BC에 없으면 비어 있다")
    void findById_legacyStubPost_returnsEmpty() {
        assertTrue(postAdapter.findById(LEGACY_STUB_POST).isEmpty());
    }

    @Test
    @DisplayName("findAllByIds는 있는 글만 바꿔 돌려주고 없는 id는 빠진다")
    void findAllByIds_mapsExistingOnly() {
        List<PostInfo> found = postAdapter.findAllByIds(List.of(POST_P, MISSING_POST, POST_Q1));

        assertEquals(2, found.size());
        assertTrue(found.contains(new PostInfo(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.AVAILABLE)));
        assertTrue(found.contains(new PostInfo(POST_Q1, "Q1", "post/20/1.jpg", MEMBER_A, PostInfo.Status.AVAILABLE)));
    }

    @Test
    @DisplayName("findAllByIds에 빈 목록이면 post를 부르지 않고 빈 목록이다")
    void findAllByIds_empty_doesNotCallPost() {
        assertTrue(postAdapter.findAllByIds(List.of()).isEmpty());
        assertTrue(postLookup.calls().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(PostInfo.Status.class)
    @DisplayName("post 상태 문자열 4개는 같은 이름의 글 상태로 바뀐다 (삭제된 글도 DELETED로 포함)")
    void findById_eachStatus_mapsSameName(PostInfo.Status status) {
        postLookup.register(POST_P, MEMBER_B, status.name(), "P", "post/10/1.jpg");

        PostInfo found = postAdapter.findById(POST_P).orElseThrow();

        assertEquals(status, found.status());
        assertEquals("P", found.title());
        assertEquals("post/10/1.jpg", found.thumbnailKey());
    }

    // ---- AC-17 알 수 없는 글 상태 ----

    @ParameterizedTest
    @ValueSource(strings = {"HIDDEN", "available", ""})
    @DisplayName("post 상태가 4개 값이 아니면 findById는 IllegalStateException이다")
    void findById_unknownStatus_throwsIllegalState(String unknownStatus) {
        postLookup.register(POST_P, MEMBER_B, unknownStatus, "P", null);

        assertThrows(IllegalStateException.class, () -> postAdapter.findById(POST_P));
    }

    @Test
    @DisplayName("여러 글 중 하나라도 상태를 모르면 findAllByIds는 IllegalStateException이다")
    void findAllByIds_oneUnknownStatus_throwsIllegalState() {
        postLookup.register(POST_Q1, MEMBER_A, "HIDDEN", "Q1", null);

        assertThrows(IllegalStateException.class, () -> postAdapter.findAllByIds(List.of(POST_P, POST_Q1)));
    }

    // ---- AC-9 · AC-13 · AC-14 상태 변경 위임 ----

    @Test
    @DisplayName("reserve는 같은 글 목록으로 post 예약중 전환을 부른다")
    void reserve_delegatesToPost() {
        postAdapter.reserve(List.of(POST_P, POST_Q1));

        assertEquals(List.of(List.of(POST_P, POST_Q1)), postStatusChanger.reserveCalls());
    }

    @Test
    @DisplayName("complete는 같은 글 목록으로 post 교환 완료 전환을 부른다")
    void complete_delegatesToPost() {
        postAdapter.complete(List.of(POST_P, POST_Q1));

        assertEquals(List.of(List.of(POST_P, POST_Q1)), postStatusChanger.completeCalls());
        assertTrue(postStatusChanger.releaseCalls().isEmpty());
    }

    @Test
    @DisplayName("release는 같은 글 목록으로 post 예약 해제를 부른다")
    void release_delegatesToPost() {
        postAdapter.release(List.of(POST_P, POST_Q1));

        assertEquals(List.of(List.of(POST_P, POST_Q1)), postStatusChanger.releaseCalls());
        assertTrue(postStatusChanger.completeCalls().isEmpty());
    }

    // ---- AC-10 · AC-16 거부 변환 ----

    @ParameterizedTest
    @EnumSource(PostStatusChangeRejectedException.Reason.class)
    @DisplayName("post가 예약중 전환을 거부하면 이유와 관계없이 거부된 글 id를 담은 PostStatusRejectedException이다")
    void reserve_rejected_translatesWithPostId(PostStatusChangeRejectedException.Reason reason) {
        postStatusChanger.rejectWith(new PostStatusChangeRejectedException(POST_Q1, reason, null));

        PostStatusRejectedException exception = assertThrows(PostStatusRejectedException.class,
                () -> postAdapter.reserve(List.of(POST_P, POST_Q1)));

        assertEquals(POST_Q1, exception.getPostId());
    }

    @Test
    @DisplayName("post가 교환 완료 전환을 거부하면 거부된 글 id를 담은 PostStatusRejectedException이다")
    void complete_rejected_translatesWithPostId() {
        postStatusChanger.rejectWith(new PostStatusChangeRejectedException(POST_P,
                PostStatusChangeRejectedException.Reason.INVALID_STATUS, null));

        PostStatusRejectedException exception = assertThrows(PostStatusRejectedException.class,
                () -> postAdapter.complete(List.of(POST_P, POST_Q1)));

        assertEquals(POST_P, exception.getPostId());
    }

    @Test
    @DisplayName("post가 예약 해제를 거부하면 거부된 글 id를 담은 PostStatusRejectedException이다")
    void release_rejected_translatesWithPostId() {
        postStatusChanger.rejectWith(new PostStatusChangeRejectedException(POST_Q1,
                PostStatusChangeRejectedException.Reason.NOT_FOUND, null));

        PostStatusRejectedException exception = assertThrows(PostStatusRejectedException.class,
                () -> postAdapter.release(List.of(POST_P, POST_Q1)));

        assertEquals(POST_Q1, exception.getPostId());
    }
}
