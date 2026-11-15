package com.ject.user.domain.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 기기마다 하나인 리프레시 토큰 (결정 C2). 토큰 원문은 갖지 않고 해시만 갖는다.
 * 시간으로는 만료되지 않고, 로그아웃 · 탈퇴 · 정지 · 재사용 감지 때만 무효가 된다 (결정 A1).
 */
public class RefreshToken {

    private final Long id;
    private Long memberId;
    private final String deviceId;
    private String tokenHash;
    private String previousTokenHash;
    private Instant rotatedAt;

    private RefreshToken(Long id, Long memberId, String deviceId, String tokenHash, String previousTokenHash,
                         Instant rotatedAt) {
        this.id = id;
        this.memberId = Objects.requireNonNull(memberId);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.previousTokenHash = previousTokenHash;
        this.rotatedAt = Objects.requireNonNull(rotatedAt);
    }

    public static RefreshToken issue(Long memberId, String deviceId, String tokenHash, Instant now) {
        return new RefreshToken(null, memberId, deviceId, tokenHash, null, now);
    }

    // 저장소에서 읽어온 데이터로 복원할 때 사용
    public static RefreshToken restore(Long id, Long memberId, String deviceId, String tokenHash,
                                       String previousTokenHash, Instant rotatedAt) {
        return new RefreshToken(id, memberId, deviceId, tokenHash, previousTokenHash, rotatedAt);
    }

    /**
     * 이 기기에서 다시 로그인 · 가입했을 때. 기기 하나에 회원 하나라서 다른 회원이 쓰던 기기여도 새 회원 것이 되고,
     * 이전 토큰은 직전 토큰으로도 남기지 않아 바로 무효가 된다 (결정 A12 · C2).
     */
    public void reissue(Long memberId, String tokenHash, Instant now) {
        this.memberId = Objects.requireNonNull(memberId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.previousTokenHash = null;
        this.rotatedAt = Objects.requireNonNull(now);
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    /** 회전 전 토큰 해시. 동시 재발급 · 재사용 감지에 쓴다 (결정 C3). 없으면 null */
    public String getPreviousTokenHash() {
        return previousTokenHash;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }
}
