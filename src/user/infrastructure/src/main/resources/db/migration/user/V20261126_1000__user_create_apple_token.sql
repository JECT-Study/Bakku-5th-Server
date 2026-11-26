-- Apple 리프레시 토큰 (암호화). 탈퇴 때 Apple 토큰을 취소하려고 보관한다 (AU-1, 애플 심사 지침 5.1.1(v))
-- 가입 전에도 받아 두므로 회원 id가 아니라 소셜 식별값 해시로 찾는다. 토큰을 취소하면 지운다
CREATE TABLE user_apple_token
(
    social_id_hash VARCHAR(64) PRIMARY KEY,
    refresh_token  TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);
