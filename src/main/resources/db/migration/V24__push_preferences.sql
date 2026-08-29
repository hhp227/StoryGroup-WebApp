-- 푸시 종류별 on/off(계정 단위) — 발송 게이트는 PushBroadcaster, 인앱 표시는 불변(설계 §1).
-- 기본 true라 기존 사용자 동작은 그대로다. 회원탈퇴 익명화는 이 컬럼을 건드리지 않는다(토큰이 삭제돼 무의미).
ALTER TABLE users ADD COLUMN push_chat_enabled     BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN push_activity_enabled BOOLEAN NOT NULL DEFAULT true;
