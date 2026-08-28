-- 푸시 토큰 저장소 — 유저당 다기기(폰+웹). token UNIQUE + upsert로 같은 기기의
-- 계정 전환 시 토큰 소유권이 새 계정으로 이관된다(이전 계정 알림 오발송 방지).
-- RLS 미적용: 로그아웃 DELETE가 소유자 무관이어야 하고(계정 전환된 기기),
-- 토큰 문자열 자체가 추측 불가능한 값이라 행 접근 통제 실익이 없다(설계 §1).
CREATE TABLE user_push_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform   VARCHAR(16) NOT NULL,
    token      VARCHAR(512) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_user_push_tokens_user ON user_push_tokens(user_id);

-- 레거시 휴면 컬럼 — 어디서도 갱신·발송에 쓰이지 않았다
ALTER TABLE users DROP COLUMN fcm_registration_id;
