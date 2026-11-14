package com.ject.user.application.dto;

/**
 * 닉네임 사용 가능 확인 결과.
 *
 * @param unavailableReason 사용할 수 없는 이유. 사용 가능하면 null
 */
public record NicknameAvailability(boolean available, UnavailableReason unavailableReason) {

    public enum UnavailableReason {
        INVALID_LENGTH,
        INVALID_FORMAT,
        DUPLICATE
    }

    public static NicknameAvailability usable() {
        return new NicknameAvailability(true, null);
    }

    public static NicknameAvailability unusable(UnavailableReason reason) {
        return new NicknameAvailability(false, reason);
    }
}
